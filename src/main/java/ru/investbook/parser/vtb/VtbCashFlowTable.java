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

package ru.investbook.parser.vtb;

import lombok.Getter;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.*;
import org.spacious_team.table_wrapper.excel.ExcelTable;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static ru.investbook.parser.vtb.VtbCashFlowTable.VtbCashFlowTableHeader.*;

public class VtbCashFlowTable extends AbstractReportTable<EventCashFlow> {

    static final String TABLE_NAME = "Движение денежных средств";

    public VtbCashFlowTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbCashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(Table table, TableRow row) {
        String operation = String.valueOf(table.getStringCellValueOrDefault(row, OPERATION, ""))
                .toLowerCase()
                .trim();
        String description = table.getStringCellValueOrDefault(row, DESCRIPTION, "");
        String lowercaseDescription = description.toLowerCase();
        CashFlowType type = switch (operation) {
            // gh-170
            case "зачисление денежных средств" -> isCash(lowercaseDescription) ? CashFlowType.CASH : null;
            case "списание денежных средств" -> CashFlowType.CASH;
            case "перевод денежных средств" -> CashFlowType.CASH; // перевод ДС на другой субсчет
            case "перераспределение дохода между субсчетами / торговыми площадками" -> CashFlowType.CASH; // выплаты субсчета проходят через основной счет
            case "ндфл" -> CashFlowType.TAX;
            default -> null;
        };
        if (type == null) {
            return Collections.emptyList();
        }
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(((ExcelTable) table).getDateCellValue(row, DATE).toInstant())
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .description(StringUtils.isEmpty(description) ? null : description)
                .build());
    }

    private boolean isCash(String lowercaseDescription) {
        return !(lowercaseDescription.contains("погаш. номин.ст-ти обл") ||
                lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч") ||
                lowercaseDescription.contains("куп. дох. по обл") ||
                lowercaseDescription.contains("дивиденды"));
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
    enum VtbCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        OPERATION("тип операции"),
        DESCRIPTION("комментарий");

        private final TableColumn column;

        VtbCashFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
