package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.TransactionEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    static final String ACCRUED_INTEREST = "Выплачены купоны (после налога)";
    static final String AMORTIZATION = "Амортизация облигации";
    static final String DIVIDEND = "Дивиденды (после налога)";
    static final String CELL_COMMISSION = "Комиссия продажи/погашения";
    static final String TAX = "Налог (с разницы курсов)";
    static final String PROFIT = "Доходность годовых, %";

    static {

    }
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;

    private final TransactionEntityConverter transactionEntityConverter;

    public List<Map<String, Object>> calculatePortfolioProfit(PortfolioEntity portfolio) {
        ArrayList<Map<String, Object>> positions = new ArrayList<>();
        for (String isin : transactionRepository.findDistinctIsinByPortfolioOrderByTimestamp(portfolio)) {
            Optional<SecurityEntity> security = securityRepository.findByIsin(isin);
            if (security.isPresent()) {
                ArrayList<Transaction> transactions = transactionRepository
                        .findBySecurityAndPortfolioOrderByTimestampAsc(security.get(), portfolio)
                        .stream()
                        .map(transactionEntityConverter::fromEntity)
                        .collect(Collectors.toCollection(ArrayList::new));
                TransactionProcessor transactionProcessor = new TransactionProcessor(transactions);
                positions.addAll(getClosedPositions(security.get(), transactionProcessor));
                positions.addAll(getUnclosedPositions(security.get(), transactionProcessor));
            }
        }
        return positions;
    }

    private List<Map<String, Object>> getClosedPositions(SecurityEntity security, TransactionProcessor transactionProcessor) {
        List<Map<String, Object>> closedPositions = new ArrayList<>();
        for (ClosedPosition position : transactionProcessor.getClosedPositions()) {
            Map<String, Object> row = getClosedPositionProfit(position);
            row.put(SECURITY, security.getName());
            closedPositions.add(row);
        }
        return closedPositions;
    }

    private List<Map<String, Object>> getUnclosedPositions(SecurityEntity security, TransactionProcessor transactionProcessor) {
        List<Map<String, Object>> unClosedPositions = new ArrayList<>();
        for (OpenedPosition position : transactionProcessor.getOpenedPositions()) {
            Map<String, Object> row = getOpenedPositionProfit(position);
            row.put(SECURITY, security.getName());
            unClosedPositions.add(row);
        }
        return unClosedPositions;
    }

    private Map<String, Object> getClosedPositionProfit(ClosedPosition position) {
        Map<String, Object> row = new HashMap<>();

        // open transaction info
        Transaction transaction = position.getOpenTransaction();
        row.put(BUY_DATE, transaction.getTimestamp());
        row.put(COUNT, position.getCount());
        row.put(BUY_PRICE, getCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(BUY_AMOUNT, getCashFlow(transaction, CashFlowType.PRICE, multipier));
        row.put(BUY_ACCRUED_INTEREST, getCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(BUY_COMMISSION, getCashFlow(transaction, CashFlowType.COMMISSION, multipier));

        // close transaction info
        transaction = position.getCloseTransaction();
        multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CELL_DATE, transaction.getTimestamp());
        row.put(CELL_AMOUNT, getCashFlow(transaction, CashFlowType.PRICE, multipier));
        row.put(CELL_ACCRUED_INTEREST, getCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(CELL_COMMISSION, getCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        return row;
    }

    private Map<String, Object> getOpenedPositionProfit(OpenedPosition position) {
        Map<String, Object> row = new HashMap<>();

        // open transaction info
        Transaction transaction = position.getTransaction();
        row.put(BUY_DATE, transaction.getTimestamp());
        row.put(COUNT, position.getUnclosedPositions());
        row.put(BUY_PRICE, getCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multipier = Math.abs(1d * position.getUnclosedPositions() / transaction.getCount());
        row.put(BUY_AMOUNT, getCashFlow(transaction, CashFlowType.PRICE, multipier));
        row.put(BUY_ACCRUED_INTEREST, getCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(BUY_COMMISSION, getCashFlow(transaction, CashFlowType.COMMISSION, multipier));

        return row;
    }

    private BigDecimal getCashFlow(Transaction transaction, CashFlowType type, double multiplier) {
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
}


