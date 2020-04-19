/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.AbstractReportTable;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class CashFlowTable extends AbstractReportTable<EventCashFlow> {

    public CashFlowTable(UralsibBrokerReport report) {
        super(report, PaymentsTable.TABLE_NAME, "", PaymentsTable.PaymentsTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> pasreTable(ExcelTable table) {
        return table.getDataCollection(getReport().getPath(), this::getRow, e ->
                // SQL db restricts storing duplicate rows. Join rows by summing they values.
                Collections.singletonList(e.toBuilder()
                        .value(e.getValue()
                                .multiply(BigDecimal.valueOf(2)))
                        .build()));
    }

    @Override
    protected Collection<EventCashFlow> getRow(ExcelTable table, Row row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        CashFlowType type;
        switch (action) {
            case"ввод дс":
            case"вывод дс":
                type = CashFlowType.CASH;
                break;
            case "налог":
                type = CashFlowType.TAX;
                break;
            case "доначисление комиссии до размера минимальной":
            case "депозитарные сборы других депозитариев":
                type = CashFlowType.COMMISSION;
                break;
            default:
                return emptyList();
        }
        String description = table.getStringCellValue(row, DESCRIPTION);
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .description((description == null || description.isEmpty())? null : description)
                .build());
    }
}
