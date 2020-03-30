package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
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
import ru.portfolio.portfolio.view.ProfitTable;
import ru.portfolio.portfolio.view.ProfitTableFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static ru.portfolio.portfolio.view.excel.DerivativesMarketExcelProfitTableHeader.*;

@Component
@RequiredArgsConstructor
public class DerivativesMarketExcelProfitTableFactory implements ProfitTableFactory {
    private static final String TAX_FORMULA = "=IF(" + DERIVATIVE_PROFIT_TOTAL.getCellAddr()
            + "<=0,0,0.13*" + DERIVATIVE_PROFIT_TOTAL.getCellAddr() +")";
    private static final String PROFIT_FORMULA = "=" + DERIVATIVE_PROFIT_TOTAL.getCellAddr()
            + "-" + COMMISSION.getCellAddr()
            + "-" + FORECAST_TAX.getCellAddr();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityEntityConverter securityEntityConverter;
    private final DerivativeCashFlowFactory derivativeCashFlowFactory;

    public ProfitTable create(PortfolioEntity portfolio) {
        ProfitTable profit = new ProfitTable();
        for (String isin : getSecuritiesIsin(portfolio)) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security security = securityEntityConverter.fromEntity(securityEntity.get());
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

    private ProfitTable getContractProfit(Security security, DerivativeCashFlow derivativeCashFlow) {
        ProfitTable contractProfit = new ProfitTable();
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        int totalContractCount = 0;
        for (DerivativeCashFlow.DailyCashFlow dailyCashFlow : derivativeCashFlow.getCashFlows()) {
            ProfitTable.Record record = new ProfitTable.Record();
            contractProfit.add(record);
            boolean isFirstRowOfDay = true;
            for (Map.Entry<Transaction, Map<CashFlowType, TransactionCashFlow>> e :
                    dailyCashFlow.getDailyTransactions().entrySet()) {
                if (!isFirstRowOfDay) {
                    record = new ProfitTable.Record();
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
        ProfitTable.Record total = new ProfitTable.Record();
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
