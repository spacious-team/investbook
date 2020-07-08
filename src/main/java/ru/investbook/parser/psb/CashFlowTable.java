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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.investbook.parser.*;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.EventCashFlow;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.CashFlowTable.CashFlowTableHeader.*;

@Slf4j
public class CashFlowTable extends AbstractReportTable<EventCashFlow> {

    private static final String TABLE_NAME = "Внешнее движение денежных средств в валюте счета";

    public CashFlowTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, "", CashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(ExcelTable table, Row row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        CashFlowType type = CashFlowType.CASH;
        boolean isPositive;
        switch (action) {
            case "зачислено на счет":
                isPositive = true;
                break;
            case "списано со счета":
                isPositive = false;
                break;
            case "налог удержанный":
                isPositive = false;
                type = CashFlowType.TAX;
                break;
            default:
                return emptyList();
        }
        if (type == CashFlowType.CASH && !table.getStringCellValue(row, DESCRIPTION).isEmpty()) {
            return emptyList(); // cash in/out records has no description
        }
        String description = table.getStringCellValue(row, DESCRIPTION);
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(table.getCurrencyCellValue(row, VALUE)
                        .multiply(BigDecimal.valueOf(isPositive ? 1 : -1)))
                .currency(table.getStringCellValue(row, CURRENCY))
                .description((description == null || description.isEmpty())? null : description)
                .build());
    }

    @Override
    protected boolean checkEquality(EventCashFlow flow1, EventCashFlow flow2) {
        return EventCashFlow.checkEquality(flow1, flow2);
    }

    @Override
    protected Collection<EventCashFlow> mergeDuplicates(EventCashFlow old, EventCashFlow nw) {
        return EventCashFlow.mergeDuplicates(old, nw);
    }

    enum CashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("операция"),
        VALUE("сумма"),
        CURRENCY("валюта счета"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;
        CashFlowTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
