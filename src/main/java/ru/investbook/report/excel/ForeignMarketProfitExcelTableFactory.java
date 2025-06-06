/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.report.excel;

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.report.ClosedPosition;
import ru.investbook.report.FifoPositions;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.report.OpenedPosition;
import ru.investbook.report.Position;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static ru.investbook.report.excel.ForeignMarketProfitExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class ForeignMarketProfitExcelTableFactory implements TableFactory {
    private final SecurityRepository securityRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final FifoPositionsFactory positionsFactory;

    public Table create(Portfolio portfolio) {
        return create(portfolio, getCurrencyPairs(portfolio));
    }

    public Table create(Portfolio portfolio, Collection<String> currencyPairs) {
        ViewFilter f = ViewFilter.get();
        FifoPositionsFilter positionsFilter = FifoPositionsFilter.of(portfolio, f.getFromDate(), f.getToDate());
        Table openPositionsProfit = new Table();
        Table closedPositionsProfit = new Table();
        for (String currencyPair : currencyPairs) {
            FifoPositions positions = positionsFactory.getForCurrencyPair(currencyPair, positionsFilter);
            openPositionsProfit.addAll(getPositionProfit(currencyPair, positions.getOpenedPositions(),
                    this::getOpenedPositionProfit));
            closedPositionsProfit.addAll(getPositionProfit(currencyPair, positions.getClosedPositions(),
                    this::getClosedPositionProfit));
        }
        Table profit = new Table();
        profit.addAll(openPositionsProfit);
        profit.addAll(closedPositionsProfit);
        return profit;
    }

    /**
     * Returns currency pairs, for example USDRUB, EURRUB
     */
    private Collection<String> getCurrencyPairs(Portfolio portfolio) {
        Collection<Integer> fxContracts = transactionRepository
                .findDistinctFxContractByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
                        singleton(portfolio.getId()),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate());
        return securityRepository.findDistinctCurrencyPair(fxContracts);
    }

    private <T extends Position> Table getPositionProfit(String currencyPair,
                                                         Deque<T> positions,
                                                         Function<T, Table.Record> profitBuilder) {
        Table rows = new Table();
        for (T position : positions) {
            Table.Record record = profitBuilder.apply(position);
            record.put(CURRENCY_PAIR, currencyPair);
            rows.add(record);
        }
        return rows;
    }

    private Table.Record getOpenedPositionProfit(OpenedPosition position) {
        Table.Record row = new Table.Record();
        Transaction transaction = position.getOpenTransaction();
        row.put(OPEN_DATE, transaction.getTimestamp());
        row.put(COUNT, Math.abs(position.getCount()) * Integer.signum(transaction.getCount()));
        //noinspection DataFlowIssue
        row.put(OPEN_PRICE, getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        //noinspection DataFlowIssue
        row.put(OPEN_AMOUNT, getTransactionCashFlow(transaction, CashFlowType.PRICE, multiplier));
        //noinspection DataFlowIssue
        row.put(OPEN_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.FEE, multiplier));
        return row;
    }

    private Table.Record getClosedPositionProfit(ClosedPosition position) {
        // open transaction info
        Table.Record row = new Table.Record(getOpenedPositionProfit(position));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multiplier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CLOSE_DATE, transaction.getTimestamp());
        @Nullable BigDecimal closeAmount;
        if (position.getClosingEvent() == CashFlowType.PRICE) {
            closeAmount = getTransactionCashFlow(transaction, CashFlowType.PRICE, multiplier);
        } else {
            throw new IllegalArgumentException("ЦБ " + transaction.getSecurity() +
                    " не может быть закрыта событием типа " + position.getClosingEvent());
        }
        row.put(CLOSE_AMOUNT, closeAmount);
        //noinspection DataFlowIssue
        row.put(CLOSE_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.FEE, multiplier));
        boolean isLongPosition = isLongPosition(position);
        row.put(FORECAST_TAX, getForecastTax(isLongPosition));
        row.put(PROFIT, getClosedPositionProfit(isLongPosition));
        row.put(YIELD, getClosedPositionYield(isLongPosition));
        return row;
    }

    private boolean isLongPosition(ClosedPosition position) {
        return position.getOpenTransaction().getCount() > 0;
    }

    private @Nullable BigDecimal getTransactionCashFlow(Transaction transaction, CashFlowType type, double multiplier) {
        @Nullable Integer transactionId = transaction.getId();
        if (transactionId == null) {
            return null;
        }
        return transactionCashFlowRepository
                .findByTransactionIdAndCashFlowType(transactionId, type)
                .map(cash -> cash.getValue()
                        .multiply(BigDecimal.valueOf(multiplier))
                        .abs()
                        .setScale(6, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private String getForecastTax(boolean isLongPosition) {
        String open = OPEN_AMOUNT.getCellAddr();
        String close = CLOSE_AMOUNT.getCellAddr();
        String commission = "(" + OPEN_COMMISSION.getCellAddr() + "+" + CLOSE_COMMISSION.getCellAddr() + ")";
        String buy = isLongPosition ? open : close;
        String cell = isLongPosition ? close : open;
        String forecastTaxFormula = cell + "-" + buy + "-" + commission;
        return "=IF(" + forecastTaxFormula + "<0,0,0.13*(" + forecastTaxFormula + "))";
    }

    private String getClosedPositionProfit(boolean isLongPosition) {
        String open = OPEN_AMOUNT.getCellAddr();
        String close = CLOSE_AMOUNT.getCellAddr();
        String openCommission = OPEN_COMMISSION.getCellAddr();
        String closeCommission = CLOSE_COMMISSION.getCellAddr();
        String commission = "(" + openCommission + "+" + closeCommission + ")";
        String tax = FORECAST_TAX.getCellAddr();

        String buy = isLongPosition ? open : close;
        String cell = isLongPosition ? close : open;

        return "=" + cell + "-" + buy + "-" + tax + "-" + commission;
    }

    private String getClosedPositionYield(boolean isLongPosition) {
        String profit = getClosedPositionProfit(isLongPosition).replace("=", "");
        String open = OPEN_AMOUNT.getCellAddr();
        String openCommission = OPEN_COMMISSION.getCellAddr();
        // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
        String multiplier = "100*365/(1+ABS(DAYS360(" + OPEN_DATE.getCellAddr() + "," + CLOSE_DATE.getCellAddr() + ")))";
        return "=(" + profit + ")" +
                "/(" + open + "+" + openCommission + ")" +
                "*" + multiplier;
    }
}
