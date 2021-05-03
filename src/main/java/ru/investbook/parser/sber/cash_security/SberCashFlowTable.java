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

package ru.investbook.parser.sber.cash_security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;

import static ru.investbook.parser.sber.cash_security.SberCashFlowTable.SberCashFlowTableHeader.*;

@Slf4j
public class SberCashFlowTable extends AbstractReportTable<EventCashFlow> {
    private static final String FIRST_LINE = "Номер договора";

    protected SberCashFlowTable(SberCashBrokerReport report) {
        super(report, "Движение ДС", FIRST_LINE, null, SberCashFlowTableHeader.class);
    }

    @Override
    protected EventCashFlow parseRow(TableRow row) {
        if (!"Исполнено".equalsIgnoreCase(row.getStringCellValueOrDefault(STATUS, null))) {
            return null;
        }
        CashFlowType type;
        boolean isNegative = false;
        String operation = row.getStringCellValue(OPERATION);
        switch (operation) {
            case "Ввод ДС" -> type = CashFlowType.CASH;
            case "Вывод ДС" -> { // предположение, нет примера отчета
                type = CashFlowType.CASH;
                isNegative = true;
            }
            case "Зачисление дивидендов" -> type = CashFlowType.DIVIDEND;
            case "Зачисление купона" -> type = CashFlowType.COUPON;
            case "Зачисление суммы от погашения ЦБ" -> type = CashFlowType.AMORTIZATION; // нет примера для REDEMPTION
            case "Списание налогов" -> {
                type = CashFlowType.TAX;
                isNegative = true;
            }
            case "Списание комиссии" -> {
                type = CashFlowType.COMMISSION;
                isNegative = true;
            }
            default -> {
                log.warn("Неизвестный тип операции: {} в отчете {}", operation, getReport());
                return null;
            }
        }
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isNegative) {
            value = value.negate();
        }
        return EventCashFlow.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .eventType(type)
                .value(value)
                .currency(row.getStringCellValue(CURRENCY))
                .description(row.getStringCellValue(DESCRIPTION))
                .build();
    }

    enum SberCashFlowTableHeader implements TableColumnDescription {
        PORTFOLIO("Номер договора"),
        DATE_TIME("Дата исполнения поручения"),
        OPERATION("Операция"),
        VALUE("Сумма"), // без учета НКД и комиссий
        CURRENCY("Валюта операции"),
        FROM("Списание с"),
        TO("Зачисление на"),
        DESCRIPTION("Содержание операции"),
        STATUS("Статус");

        @Getter
        private final TableColumn column;
        SberCashFlowTableHeader(String words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
