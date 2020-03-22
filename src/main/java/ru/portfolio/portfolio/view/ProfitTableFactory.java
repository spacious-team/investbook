package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.converter.TransactionEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.StockMarketExcelProfitTableHeader.SECURITY;

@RequiredArgsConstructor
public abstract class ProfitTableFactory {
    protected final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final TransactionEntityConverter transactionEntityConverter;
    private final SecurityEntityConverter securityEntityConverter;
    private final PaidInterestFactory paidInterestFactory;

    public ProfitTable create(PortfolioEntity portfolio) {
        ProfitTable openPositionsProfit = new ProfitTable();
        ProfitTable closedPositionsProfit = new ProfitTable();
        for (String isin : getSecuritiesIsin(portfolio)) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security security = securityEntityConverter.fromEntity(securityEntity.get());
                Positions positions = getPositions(portfolio, securityEntity.get());
                PaidInterest paidInterest = paidInterestFactory.create(portfolio.getPortfolio(), security, positions);
                closedPositionsProfit.addAll(getPositionProfit(security, positions.getClosedPositions(),
                        paidInterest, this::getClosedPositionProfit));
                openPositionsProfit.addAll(getPositionProfit(security, positions.getOpenedPositions(),
                        paidInterest, this::getOpenedPositionProfit));
            }
        }
        ProfitTable profit = new ProfitTable();
        profit.addAll(closedPositionsProfit);
        profit.addAll(openPositionsProfit);
        return profit;
    }

    protected abstract Collection<String> getSecuritiesIsin(PortfolioEntity portfolio);

    private Positions getPositions(PortfolioEntity portfolio, SecurityEntity securityEntity) {
        Deque<Transaction> transactions = transactionRepository
                .findBySecurityAndPortfolioOrderByTimestampAscIdAsc(securityEntity, portfolio)
                .stream()
                .map(transactionEntityConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
        Deque<SecurityEventCashFlow> redemption = getRedemption(portfolio, securityEntity);
        return new Positions(transactions, redemption);
    }

    protected abstract Deque<SecurityEventCashFlow> getRedemption(PortfolioEntity portfolio,
                                                                  SecurityEntity securityEntity);

    private <T extends Position> ProfitTable getPositionProfit(Security security,
                                                               Deque<T> positions,
                                                               PaidInterest paidInterest,
                                                               Function<T, ProfitTable.Record> profitBuilder) {
        ProfitTable rows = new ProfitTable();
        for (T position : positions) {
            ProfitTable.Record record = profitBuilder.apply(position);
            record.putAll(getPaidInterestProfit(position, paidInterest));
            record.put(SECURITY,
                    Optional.ofNullable(security.getName())
                            .orElse(security.getIsin()));
            rows.add(record);
        }
        return rows;
    }

    protected abstract ProfitTable.Record getOpenedPositionProfit(OpenedPosition position);

    protected abstract ProfitTable.Record getClosedPositionProfit(ClosedPosition position);

    protected abstract ProfitTable.Record getPaidInterestProfit(Position position,
                                                                PaidInterest paidInterest);

    protected BigDecimal getTransactionCashFlow(Transaction transaction, CashFlowType type, double multiplier) {
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
