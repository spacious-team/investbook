/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.StringUtils;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.CashFlowTable.CashFlowTableHeader.*;

@Slf4j
public class CashFlowTable extends SingleAbstractReportTable<EventCashFlow> {

    private static final String TABLE_NAME = "Внешнее движение денежных средств в валюте счета";

    public CashFlowTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, "", CashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(TableRow row) {
        String action = row.getStringCellValue(OPERATION);
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
        if (type == CashFlowType.CASH && !row.getStringCellValue(DESCRIPTION).isEmpty()) {
            return emptyList(); // cash in/out records has no description
        }
        String description = row.getStringCellValueOrDefault(DESCRIPTION, null);
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .value(row.getBigDecimalCellValue(VALUE)
                        .multiply(BigDecimal.valueOf(isPositive ? 1 : -1)))
                .currency(row.getStringCellValue(CURRENCY))
                .description(StringUtils.hasLength(description) ? description : null)
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
