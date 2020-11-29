/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.foreignmarket.ForeignExchangeCashFlowTable.FxCashFlowTableHeader.*;

@Slf4j
public class ForeignExchangeCashFlowTable extends AbstractReportTable<EventCashFlow> {

    private static final String TABLE_NAME = "Информация об операциях с активами";
    private static final String BROKER_COMMISSION = "Комиссия брокера";
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
    protected Collection<EventCashFlow> getRow(Table table, TableRow row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        boolean isPositive;
        switch (action) {
            case "ввод дс":
                isPositive = true;
                break;
            case "вывод дс":
                isPositive = false;
                break;
            default:
                log.debug("Не известный тип операции '{}' в таблице '{}'", action, table);
                return emptyList();
        }
        String currency;
        BigDecimal value = table.getCurrencyCellValue(row, RUB);
        if (value.compareTo(min) > 0) {
            currency = "RUB";
        } else {
            value = table.getCurrencyCellValue(row, USD);
            if (value.compareTo(min) > 0) {
                currency = "USD";
            } else {
                value = table.getCurrencyCellValue(row, EUR);
                if (value.compareTo(min) > 0) {
                    currency = "EUR";
                } else {
                    return emptyList();
                }
            }
        }
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(CashFlowType.CASH)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(value.multiply(BigDecimal.valueOf(isPositive ? 1 : -1)))
                .currency(currency)
                .description("Операция по валютному счету")
                .build());
    }

    private Collection<EventCashFlow> getDailyBrokerCommission() {
        try {
            Object value = getReport().getReportPage().getNextColumnValue(BROKER_COMMISSION);
            double doubleValue = Double.parseDouble(String.valueOf(value));
            if (doubleValue > 0.01) {
                BigDecimal brockerCommission = BigDecimal.valueOf(doubleValue).negate();
                return singletonList(EventCashFlow.builder()
                        .portfolio(getReport().getPortfolio())
                        .eventType(CashFlowType.COMMISSION)
                        .timestamp(getReport().getReportEndDateTime())
                        .value(brockerCommission)
                        .currency("RUB")
                        .description("Комиссия брокера за обналичивание валюты")
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

    enum FxCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("вид"),
        RUB("RUB"),
        USD("USD"),
        EUR("EUR");

        @Getter
        private final TableColumn column;
        FxCashFlowTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
