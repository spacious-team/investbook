package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.converter.SecurityEventCashFlowEntityConverter;
import ru.portfolio.portfolio.converter.TransactionEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TransactionProfitTableFactory {
    static final String SECURITY = "Бумага";
    static final String BUY_DATE = "Дата покупки";
    static final String COUNT = "Количество";
    static final String BUY_PRICE = "Цена, % номинала/руб";
    static final String BUY_AMOUNT = "Стоимость (без НКД и комиссии)";
    static final String BUY_ACCRUED_INTEREST = "НКД уплоченный";
    static final String BUY_COMMISSION = "Комиссия покупки";
    static final String CELL_DATE = "Дата продажи";
    static final String CELL_AMOUNT = "Стоимость прод/погаш";
    static final String CELL_ACCRUED_INTEREST = "НКД при продаже" ;
    static final String COUPON = "Выплачены купоны (после налога)";
    static final String AMORTIZATION = "Амортизация облигации";
    static final String DIVIDEND = "Дивиденды (после налога)";
    static final String CELL_COMMISSION = "Комиссия продажи/погашения";
    static final String TAX = "Налог (с разницы курсов)";
    static final String PROFIT = "Доходность годовых, %";

    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionEntityConverter transactionEntityConverter;
    private final SecurityEntityConverter securityEntityConverter;
    private final SecurityEventCashFlowEntityConverter securityEventCashFlowEntityConverter;
    private final PaidInterestFactory paidInterestFactory;

    public List<Map<String, Object>> calculatePortfolioProfit(PortfolioEntity portfolio) {
        ArrayList<Map<String, Object>> openPositionsProfit = new ArrayList<>();
        ArrayList<Map<String, Object>> closedPositionsProfit = new ArrayList<>();
        for (String isin : transactionRepository.findDistinctIsinByPortfolioOrderByTimestamp(portfolio)) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security security = securityEntityConverter.fromEntity(securityEntity.get());
                Deque<Transaction> transactions = transactionRepository
                        .findBySecurityAndPortfolioOrderByTimestampAscIdAsc(securityEntity.get(), portfolio)
                        .stream()
                        .map(transactionEntityConverter::fromEntity)
                        .collect(Collectors.toCollection(LinkedList::new));
                Deque<SecurityEventCashFlow> redemption = securityEventCashFlowRepository
                        .findByIsinAndCashFlowTypeOOrderByTimestampAsc(isin, CashFlowType.REDEMPTION)
                        .stream()
                        .map(securityEventCashFlowEntityConverter::fromEntity)
                        .collect(Collectors.toCollection(LinkedList::new));
                Positions positions = new Positions(transactions, redemption);
                PaidInterest paidInterest = paidInterestFactory.getPayedInterestFor(security, positions);
                closedPositionsProfit.addAll(getPositionProfit(security, positions.getClosedPositions(), paidInterest,this::getClosedPositionProfit));
                openPositionsProfit.addAll(getPositionProfit(security, positions.getOpenedPositions(), paidInterest, this::getOpenedPositionProfit));
            }
        }
        ArrayList<Map<String, Object>> profit = new ArrayList<>(closedPositionsProfit);
        profit.addAll(openPositionsProfit);
        return profit;
    }

    private <T extends Position> List<Map<String, Object>> getPositionProfit(Security security,
                                                                             Deque<T> positions,
                                                                             PaidInterest paidInterest,
                                                                             Function<T, Map<String, Object>> profitBuilder) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (T position : positions) {
            Map<String, Object> row = profitBuilder.apply(position);
            row.put(SECURITY, security.getName());
            row.put(COUPON, paidInterest.get(CashFlowType.COUPON, position));
            row.put(AMORTIZATION, paidInterest.get(CashFlowType.AMORTIZATION, position));
            row.put(DIVIDEND, paidInterest.get(CashFlowType.DIVIDEND, position));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> getOpenedPositionProfit(OpenedPosition position) {
        Map<String, Object> row = new HashMap<>();
        Transaction transaction = position.getOpenTransaction();
        row.put(BUY_DATE, transaction.getTimestamp());
        row.put(COUNT, position.getCount());
        row.put(BUY_PRICE, getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(BUY_AMOUNT, getTransactionCashFlow(transaction, CashFlowType.PRICE, multipier));
        row.put(BUY_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(BUY_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        return row;
    }

    private Map<String, Object> getClosedPositionProfit(ClosedPosition position) {
        // open transaction info
        Map<String, Object> row = new HashMap<>(getOpenedPositionProfit(position));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CELL_DATE, transaction.getTimestamp());
        BigDecimal cellAmount;
        switch (position.getClosingEvent()) {
            case PRICE:
                cellAmount = getTransactionCashFlow(transaction, CashFlowType.PRICE, multipier);
                break;
            case REDEMPTION:
                cellAmount =getRedemptionCashFlow(transaction.getIsin(), multipier);
                break;
            default:
                throw new IllegalArgumentException("ЦБ " + transaction.getIsin() +
                        " не может быть закрыта событием типа " + position.getClosingEvent());
        }
        row.put(CELL_AMOUNT, cellAmount);
        row.put(CELL_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(CELL_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        return row;
    }

    private BigDecimal getTransactionCashFlow(Transaction transaction, CashFlowType type, double multiplier) {
        if (transaction.getId() == null) {
            return null;
        }
        Optional<TransactionCashFlowEntity> cashFlow = transactionCashFlowRepository
                .findByTransactionIdAndCashFlowType(transaction.getId(), type);
        return cashFlow.map(cash -> cash
                .getValue()
                .multiply(BigDecimal.valueOf(multiplier))
                .abs()
                .setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private BigDecimal getRedemptionCashFlow(String isin, double multiplier) {
        List<SecurityEventCashFlowEntity> cashFlows = securityEventCashFlowRepository
                .findByIsinAndCashFlowType(isin, CashFlowType.REDEMPTION);
        if (cashFlows.isEmpty()) {
            return null;
        } else if (cashFlows.size() != 1) {
            throw new IllegalArgumentException("По ЦБ может быть не более одного события погашения, по бумаге " + isin +
                    " найдено " + cashFlows.size() + " событий погашения: " + cashFlows);
        }
        return cashFlows.get(0)
                .getValue()
                .multiply(BigDecimal.valueOf(multiplier))
                .abs()
                .setScale(2, RoundingMode.HALF_UP);
    }
}
