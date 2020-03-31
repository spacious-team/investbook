package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.pojo.TransactionCashFlow;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;
import ru.portfolio.portfolio.view.DerivativeCashFlow;
import ru.portfolio.portfolio.view.DerivativeCashFlowFactory;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static ru.portfolio.portfolio.view.excel.DerivativesMarketProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class DerivativesMarketProfitExcelTableFactory implements TableFactory {
    private static final String TAX_FORMULA = "=IF(" + DERIVATIVE_PROFIT_TOTAL.getCellAddr()
            + "<=0,0,0.13*" + DERIVATIVE_PROFIT_TOTAL.getCellAddr() +")";
    private static final String PROFIT_FORMULA = "=" + DERIVATIVE_PROFIT_TOTAL.getCellAddr()
            + "-" + COMMISSION.getCellAddr()
            + "-" + FORECAST_TAX.getCellAddr();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;
    private final DerivativeCashFlowFactory derivativeCashFlowFactory;

    public Table create(PortfolioEntity portfolio) {
        Table profit = new Table();
        for (String isin : getSecuritiesIsin(portfolio)) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security security = securityConverter.fromEntity(securityEntity.get());
                DerivativeCashFlow derivativeCashFlow = derivativeCashFlowFactory.getDerivativeCashFlow(portfolio, securityEntity.get());

                profit.addEmptyRecord();
                profit.addAll(getContractProfit(security, derivativeCashFlow));
            }
        }
        return profit;
    }

    private Collection<String> getSecuritiesIsin(PortfolioEntity portfolio) {
        return transactionRepository.findDistinctDerivativeByPortfolioOrderByTimestampDesc(portfolio);
    }

    private Table getContractProfit(Security security, DerivativeCashFlow derivativeCashFlow) {
        Table contractProfit = new Table();
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalContractCount = 0;
        for (DerivativeCashFlow.DailyCashFlow dailyCashFlow : derivativeCashFlow.getCashFlows()) {
            Table.Record record = new Table.Record();
            contractProfit.add(record);
            boolean isFirstRowOfDay = true;
            for (Map.Entry<Transaction, Map<CashFlowType, TransactionCashFlow>> e :
                    dailyCashFlow.getDailyTransactions().entrySet()) {
                if (!isFirstRowOfDay) {
                    record = new Table.Record();
                    contractProfit.add(record);
                }
                Transaction transaction = e.getKey();
                Map<CashFlowType, TransactionCashFlow> transactionCashFlows = e.getValue();
                record.put(DATE, transaction.getTimestamp());
                record.put(DIRECTION, (transaction.getCount() > 0) ? "покупка" : "продажа");
                record.put(COUNT, Math.abs(transaction.getCount()));
                record.put(QUOTE, Optional.ofNullable(transactionCashFlows.get(CashFlowType.DERIVATIVE_QUOTE))
                        .map(TransactionCashFlow::getValue)
                        .orElse(null));
                record.put(AMOUNT, Optional.ofNullable(transactionCashFlows.get(CashFlowType.DERIVATIVE_PRICE))
                        .map(TransactionCashFlow::getValue)
                        .orElse(null));
                BigDecimal commission = Optional.ofNullable(transactionCashFlows.get(CashFlowType.COMMISSION))
                        .map(TransactionCashFlow::getValue)
                        .map(BigDecimal::abs)
                        .orElse(BigDecimal.ZERO);
                totalCommission = totalCommission.add(commission);
                record.put(COMMISSION, commission);
                isFirstRowOfDay = false;
            }
            record.put(DATE, dailyCashFlow.getDailyProfit().getTimestamp());
            record.put(DERIVATIVE_PROFIT_DAY, dailyCashFlow.getDailyProfit().getValue());
            totalProfit = dailyCashFlow.getTotalProfit();
            record.put(DERIVATIVE_PROFIT_TOTAL, totalProfit);
            totalContractCount = dailyCashFlow.getPosition();
            record.put(POSITION, totalContractCount);
        }
        Table.Record total = new Table.Record();
        total.put(CONTRACT, security.getIsin());
        total.put(DIRECTION, "Итого");
        total.put(COUNT, totalContractCount);
        total.put(COMMISSION, totalCommission);
        total.put(DERIVATIVE_PROFIT_TOTAL, totalProfit);
        total.put(FORECAST_TAX, TAX_FORMULA);
        total.put(PROFIT, PROFIT_FORMULA);
        contractProfit.addFirst(total);

        return contractProfit;
    }
}
