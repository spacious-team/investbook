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

package ru.investbook.parser.investbook;

import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.Optional;

import static org.spacious_team.broker.pojo.CashFlowType.CASH;
import static org.spacious_team.broker.pojo.CashFlowType.TAX;
import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookCashFlowTable extends AbstractInvestbookTable<EventCashFlow> {

    protected InvestbookCashFlowTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected EventCashFlow parseRow(TableRow row) {
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        CashFlowType type;
        boolean negate;
        if (operation.contains("налог")) { // Налог / Возврат налога на банковский счет / Налоговый вычет
            type = (operation.contains("возврат") || operation.contains("вычет")) ? CASH : TAX;
            negate = true;
        } else if (operation.contains("комис")) { // Комиссия / Возврат комиссии
            type = CashFlowType.FEE;
            negate = !operation.contains("возврат");
        } else if (operation.contains("ввод")) { // Ввод денежных средств
            type = CASH;
            negate = false;
        } else if (operation.contains("вывод")) { // Вывод денежных средств
            type = CASH;
            negate = true;
        } else {
            return null;
        }
        BigDecimal value = Optional.ofNullable(row.getBigDecimalCellValueOrDefault(PRICE, null))
                .orElseGet(() -> row.getBigDecimalCellValue(FEE))
                .abs();
        if (negate) value = value.negate();
        String currency = Optional.ofNullable(row.getStringCellValueOrDefault(CURRENCY, null))
                .orElseGet(() -> row.getStringCellValue(FEE_CURRENCY));
        return EventCashFlow.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .eventType(type)
                .value(value)
                .currency(currency)
                .build();
    }
}
