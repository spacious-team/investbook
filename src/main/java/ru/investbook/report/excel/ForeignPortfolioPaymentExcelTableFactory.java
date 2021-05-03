/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.EventCashFlowRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.spacious_team.broker.pojo.CashFlowType.*;
import static ru.investbook.report.excel.ForeignPortfolioPaymentExcelTableHeader.*;
import static ru.investbook.report.excel.TaxExcelTableFactory.isDividendOrCouponTax;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForeignPortfolioPaymentExcelTableFactory implements TableFactory {
    /** tax accounted by {@link TaxExcelTableFactory} */
    private final Collection<Integer> PAY_TYPES = Stream.of(new CashFlowType[]{AMORTIZATION, REDEMPTION, COUPON, DIVIDEND, TAX})
            .map(CashFlowType::getId)
            .collect(Collectors.toList());
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;

    @Override
    public Table create(Portfolio portfolio) {
        List<EventCashFlow> cashFlows = getCashFlows(portfolio);
        return getTable(cashFlows);
    }

    private ArrayList<EventCashFlow> getCashFlows(Portfolio portfolio) {
        return eventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdInAndTimestampBetweenOrderByTimestampDesc(
                        portfolio.getId(),
                        PAY_TYPES,
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Table getTable(List<EventCashFlow> cashFlows) {
        Table table = new Table();
        cashFlows = cashFlows.stream()
                .filter(cash -> cash.getEventType() != TAX || isDividendOrCouponTax(cash.getDescription()))
                .collect(Collectors.toList());
        if (!cashFlows.isEmpty()) {
            table.add(new Table.Record());
            Table.Record monthTotalRecord = new Table.Record();
            table.add(monthTotalRecord);
            Month month = null;
            int sumRowCount = 0;
            for (EventCashFlow cash : cashFlows) {
                Instant timestamp = cash.getTimestamp();
                Month currentMonth = LocalDate.ofInstant(timestamp, ZoneId.systemDefault()).getMonth();
                if (month == null) {
                    month = currentMonth;
                } else if (currentMonth != month) {
                    calcTotalRecord(monthTotalRecord, month, sumRowCount);
                    table.add(new Table.Record());
                    monthTotalRecord = new Table.Record();
                    table.add(monthTotalRecord);
                    month = currentMonth;
                    sumRowCount = 0;
                }
                Table.Record record = new Table.Record();
                record.put(DATE, timestamp);
                record.put(ForeignPortfolioPaymentExcelTableHeader.CASH, cash.getValue());
                record.put(CURRENCY, cash.getCurrency());
                record.put(CASH_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(),
                        ForeignPortfolioPaymentExcelTableHeader.CASH, EXCHANGE_RATE));
                record.put(DESCRIPTION, cash.getDescription());
                table.add(record);
                sumRowCount++;
            }
            calcTotalRecord(monthTotalRecord, month, sumRowCount);
            if (!cashFlows.isEmpty()) {
                foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
            }
        }
        return table;
    }

    private void calcTotalRecord(Table.Record monthTotalRecord, Month month, int sumRowCount) {
        if (sumRowCount != 0) {
            monthTotalRecord.put(DATE, StringUtils.capitalize(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())));
            monthTotalRecord.put(CASH_RUB, "=SUM(OFFSET(" + CASH_RUB.getCellAddr() + ",1,0," + sumRowCount + ",1))");
        }
    }
}
