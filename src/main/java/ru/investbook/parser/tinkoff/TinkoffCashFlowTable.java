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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.Collection;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.TinkoffCashFlowTable.CashFlowTableHeader.*;

public class TinkoffCashFlowTable extends SingleAbstractReportTable<EventCashFlow> {

    private String currency = "";

    protected TinkoffCashFlowTable(SingleBrokerReport report) {
        super(report,
                // Таблица не имеет собственного названия, поэтому ищем предыдущую таблицу,
                // строки чужой таблицы пропускаются
                cell -> cell.startsWith("2. Операции с денежными средствами"),
                cell -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                CashFlowTableHeader.class);
    }

    @Override
    protected @Nullable EventCashFlow parseRow(TableRow row) {
        currency = TinkoffCashFlowTableHelper.getCurrency(row, currency);
        if (!hasLength(currency)) {
            return null;
        }
        String operationOriginal = row.getStringCellValueOrDefault(OPERATION, "");
        String operation = operationOriginal.toLowerCase();
        if (!hasLength(operation) || operation.equalsIgnoreCase("Операция")) {
            return null;
        }

        if (operation.contains("пополнение счета")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.CASH)
                    .value(row.getBigDecimalCellValue(DEPOSIT))
                    .build();
        } else if (operation.contains("вывод средств")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.CASH)
                    .value(row.getBigDecimalCellValue(WITHDRAWAL).negate())
                    .build();
        } else if (operation.equalsIgnoreCase("налог")) { // следует отличать от "Налог (дивиденды)"
            return getBuilder(row, currency)
                    .eventType(CashFlowType.TAX)
                    .value(row.getBigDecimalCellValue(WITHDRAWAL).negate())
                    .build();
        } else if (operation.contains("комиссия по тарифу")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.FEE)
                    .value(row.getBigDecimalCellValue(WITHDRAWAL).negate())
                    .description(operationOriginal)
                    .build();
        }

        return null;
    }

    private EventCashFlow.EventCashFlowBuilder getBuilder(TableRow row, String currency) {
        @Nullable String description = row.getStringCellValueOrDefault(DESCRIPTION, null);
        return EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().convertToInstant(row.getStringCellValue(DATE)))
                .currency(currency)
                .description(hasLength(description) ? description : null);
    }

    @Override
    protected boolean checkEquality(EventCashFlow flow1, EventCashFlow flow2) {
        return EventCashFlow.checkEquality(flow1, flow2);
    }

    @Override
    protected Collection<EventCashFlow> mergeDuplicates(EventCashFlow old, EventCashFlow nw) {
        return EventCashFlow.mergeDuplicates(old, nw);
    }

    /**
     * Таблица не имеет собственного названия, поэтому ищем предыдущую таблицу, используется ее заголовок
     */
    @RequiredArgsConstructor
    protected enum CashFlowTableHeader implements TableHeaderColumn {
        CURRENCY("Валюта"),
        DATE("^Исходящий", "остаток"), // Дата исполнения
        OPERATION("Плановый", "исходящий", "остаток"), // Операция
        DEPOSIT(AnyOfTableColumn.of(                          // Сумма зачисления
                PatternTableColumn.of("Задолженность", "клиента", "перед", "брокером"), // отчет 2022-2024 г.
                PatternTableColumn.of("Задолженность", "клиента$"))),                   // отчет 2025 г.
        WITHDRAWAL(AnyOfTableColumn.of(                       // Сумма списания
                PatternTableColumn.of("Сумма", "непокрытого", "остатка"),  // отчет 2022-2024 г.
                PatternTableColumn.of("Непокрытая", "позиция"))),          // отчет 2025 г.
        DESCRIPTION("Задолженность", "клиента", "перед", "Депозитарием");  // Примечание

        @Getter
        private final TableColumn column;

        CashFlowTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
