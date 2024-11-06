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

package ru.investbook.parser.psb.foreignmarket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.foreignmarket.ForeignExchangeCashFlowTable.FxCashFlowTableHeader.*;
import static ru.investbook.parser.psb.foreignmarket.PsbBrokerForeignMarketReport.convertToCurrency;

@Slf4j
public class ForeignExchangeCashFlowTable extends SingleAbstractReportTable<EventCashFlow> {

    private static final String TABLE_NAME = "Информация об операциях с активами";
    private static final String BROKER_FEE = "Комиссия брокера";
    private final BigDecimal min = BigDecimal.valueOf(0.01);

    public ForeignExchangeCashFlowTable(PsbBrokerForeignMarketReport report) {
        super(report, TABLE_NAME, "", FxCashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> parseTable(Table table) {
        Collection<EventCashFlow> cashFlows = super.parseTable(table);
        cashFlows.addAll(getDailyBrokerCommission());
        return cashFlows;
    }

    @Override
    protected Collection<EventCashFlow> parseRowToCollection(TableRow row) {
        String action = row.getStringCellValue(OPERATION);
        action = action.toLowerCase().trim();
        boolean isPositive;
        switch (action) {
            case "ввод дс" -> isPositive = true;
            case "вывод дс" -> isPositive = false;
            default -> {
                log.debug("Не известный тип операции '{}' в таблице '{}'", action, row.getTable());
                return List.of();
            }
        }
        EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(CashFlowType.CASH)
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .description("Операция по валютному счету");

        Collection<EventCashFlow> values = new ArrayList<>();
        getBigDecimal(row, RUB, isPositive)
                .map(value -> builder.value(value).currency("RUB").build())
                .ifPresent(values::add);
        getBigDecimal(row, USD, isPositive)
                .map(value -> builder.value(value).currency("USD").build())
                .ifPresent(values::add);
        getBigDecimal(row, EUR, isPositive)
                .map(value -> builder.value(value).currency("EUR").build())
                .ifPresent(values::add);
        getBigDecimal(row, VALUE, isPositive)
                .map(value -> builder.value(value).currency(convertToCurrency(row.getStringCellValue(CURRENCY))).build())
                .ifPresent(values::add);

        return values;
    }

    private Optional<BigDecimal> getBigDecimal(TableRow row, FxCashFlowTableHeader column, boolean isPositive) {
        //noinspection DataFlowIssue
        return Optional.ofNullable(row.getBigDecimalCellValueOrDefault(column, null))
                .filter(value -> value.compareTo(min) > 0)
                .map(value -> isPositive ? value : value.negate());
    }

    private Collection<EventCashFlow> getDailyBrokerCommission() {
        try {
            @Nullable Object value = getReport().getReportPage().getNextColumnValue(BROKER_FEE);
            double doubleValue = Double.parseDouble(String.valueOf(value));
            if (doubleValue > 0.01) {
                BigDecimal brokerFee = BigDecimal.valueOf(doubleValue).negate();
                return singletonList(EventCashFlow.builder()
                        .portfolio(getReport().getPortfolio())
                        .eventType(CashFlowType.FEE)
                        .timestamp(getReport().getReportEndDateTime())
                        .value(brokerFee)
                        .currency("RUB")
                        .description("Комиссия на валютном рынке")
                        .build());
            }
        } catch (Exception ignore) {
        }
        return emptyList();
    }

    @Override
    protected boolean checkEquality(EventCashFlow flow1, EventCashFlow flow2) {
        return EventCashFlow.checkEquality(flow1, flow2);
    }

    @Override
    protected Collection<EventCashFlow> mergeDuplicates(EventCashFlow old, EventCashFlow nw) {
        return EventCashFlow.mergeDuplicates(old, nw);
    }

    @Getter
    @RequiredArgsConstructor
    enum FxCashFlowTableHeader implements TableHeaderColumn {
        DATE("дата"),
        OPERATION("вид"),
        RUB(OptionalTableColumn.of(PatternTableColumn.of("RUB"))),  // old format
        USD(OptionalTableColumn.of(PatternTableColumn.of("USD"))),  // old format
        EUR(OptionalTableColumn.of(PatternTableColumn.of("EUR"))),  // old format
        VALUE(OptionalTableColumn.of(PatternTableColumn.of("Сумма"))),     // new format
        CURRENCY(OptionalTableColumn.of(PatternTableColumn.of("Валюта"))); // new format

        private final TableColumn column;

        FxCashFlowTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
