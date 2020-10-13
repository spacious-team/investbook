/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.pojo.*;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.view.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static ru.investbook.view.excel.DerivativesMarketProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class DerivativesMarketProfitExcelTableFactory implements TableFactory {
    private static final String TAX_FORMULA = "=IF((" + DERIVATIVE_PROFIT_TOTAL.getCellAddr() + "-" + COMMISSION.getCellAddr() + ")<=0," +
            "0,0.13*(" + DERIVATIVE_PROFIT_TOTAL.getCellAddr() + "-" + COMMISSION.getCellAddr() +"))";
    private static final String PROFIT_FORMULA = "=" + DERIVATIVE_PROFIT_TOTAL.getCellAddr()
            + "-" + COMMISSION.getCellAddr()
            + "-" + FORECAST_TAX.getCellAddr();
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;
    private final DerivativeEventsFactory derivativeEventsFactory;

    public Table create(Portfolio portfolio) {
        Table profit = new Table();
        for (String isin : getSecuritiesIsin(portfolio)) {
            Optional<SecurityEntity> securityEntity = securityRepository.findByIsin(isin);
            if (securityEntity.isPresent()) {
                Security contract = securityConverter.fromEntity(securityEntity.get());
                DerivativeEvents derivativeEvents = derivativeEventsFactory.getDerivativeEvents(
                        portfolio,
                        contract,
                        ViewFilter.get());

                profit.addEmptyRecord();
                profit.addAll(getContractProfit(contract, derivativeEvents));
            }
        }
        return profit;
    }

    private Collection<String> getSecuritiesIsin(Portfolio portfolio) {
        return transactionRepository.findDistinctDerivativeByPortfolioAndTimestampBetweenOrderByTimestampDesc(
                portfolio,
                ViewFilter.get().getFromDate(),
                ViewFilter.get().getToDate());
    }

    private Table getContractProfit(Security contract, DerivativeEvents derivativeEvents) {
        Table contractProfit = new Table();
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalProfitInPoints = BigDecimal.ZERO;
        int totalContractCount = 0;
        for (DerivativeEvents.DerivativeDailyEvents dailyEvents : derivativeEvents.getDerivativeDailyEvents()) {
            Table.Record record = new Table.Record();
            contractProfit.add(record);
            LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactions = dailyEvents.getDailyTransactions();
            if (dailyTransactions != null) {
                boolean isFirstRowOfDay = true;
                for (Map.Entry<Transaction, Map<CashFlowType, TransactionCashFlow>> e : dailyTransactions.entrySet()) {
                    if (!isFirstRowOfDay) {
                        record = new Table.Record();
                        contractProfit.add(record);
                    }
                    Transaction transaction = e.getKey();
                    Map<CashFlowType, TransactionCashFlow> transactionCashFlows = e.getValue();
                    record.put(DATE, transaction.getTimestamp());
                    record.put(DIRECTION, (transaction.getCount() > 0) ? "покупка" : "продажа");
                    record.put(COUNT, Math.abs(transaction.getCount()));
                    Optional<BigDecimal> amountInQuotes = Optional.ofNullable(transactionCashFlows.get(CashFlowType.DERIVATIVE_QUOTE))
                            .map(TransactionCashFlow::getValue);
                    totalProfitInPoints = amountInQuotes
                            .map(totalProfitInPoints::add)
                            .orElse(totalProfitInPoints);
                    record.put(QUOTE, amountInQuotes
                            .map(q -> "=" + q + "/" + Math.abs(transaction.getCount()))
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
            }
            SecurityEventCashFlow dailyProfit = dailyEvents.getDailyProfit();
            if (dailyProfit != null) {
                record.put(DATE, dailyProfit.getTimestamp());
                record.put(DERIVATIVE_PROFIT_DAY, dailyProfit.getValue());
            }
            totalProfit = dailyEvents.getTotalProfit();
            record.put(DERIVATIVE_PROFIT_TOTAL, totalProfit);
            totalContractCount = dailyEvents.getPosition();
            record.put(POSITION, totalContractCount);
        }
        Table.Record total = new Table.Record();
        total.put(CONTRACT, contract.getIsin());
        total.put(DIRECTION, "Итого");
        total.put(COUNT, totalContractCount);
        total.put(COMMISSION, totalCommission);
        total.put(QUOTE, totalProfitInPoints);
        total.put(DERIVATIVE_PROFIT_TOTAL, totalProfit);
        total.put(FORECAST_TAX, TAX_FORMULA);
        total.put(PROFIT, PROFIT_FORMULA);
        contractProfit.addFirst(total);

        return contractProfit;
    }
}
