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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelTable;
import ru.investbook.parser.vtb.VtbCashFlowTable.VtbCashFlowTableHeader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;
import static ru.investbook.parser.vtb.VtbCashFlowTable.VtbCashFlowTableHeader.*;

@Slf4j
public class VtbDividendTable extends AbstractReportTable<EventCashFlow> {

    private static final Pattern taxInformationPattern = Pattern.compile("удержан налог (в размере )?([0-9.]+)([^.]+)");

    protected VtbDividendTable(BrokerReport report) {
        super(report, VtbCashFlowTable.TABLE_NAME, null, VtbCashFlowTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(Table table, TableRow row) {
        String operation = String.valueOf(table.getStringCellValueOrDefault(row, OPERATION, ""))
                .trim();
        if (!operation.equalsIgnoreCase("Дивиденды")) {
            return Collections.emptyList();
        }
        String description = table.getStringCellValue(row, DESCRIPTION);

        Collection<EventCashFlow> data = new ArrayList<>();
        BigDecimal tax = getTax(description.toLowerCase());
        BigDecimal value = table.getCurrencyCellValue(row, VALUE)
                .add(tax.abs());
        EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(CashFlowType.DIVIDEND)
                .timestamp(((ExcelTable) table).getDateCellValue(row, DATE).toInstant())
                .value(value)
                .currency(VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .description(description);
        data.add(builder.build());
        if (tax.abs().compareTo(minValue) >= 0) {
            data.add(builder
                    .eventType(CashFlowType.TAX)
                    .value(tax.negate())
                    .build());
        }
        return data;
    }

    static BigDecimal getTax(String description) {
        Matcher matcher = taxInformationPattern.matcher(description);
        if (matcher.find()) {
            try {
                return BigDecimal.valueOf(parseDouble(matcher.group(2)));
            } catch (Exception e) {
                log.info("Не смогу выделить сумму налога из описания: {}", description);
            }
        }
        return BigDecimal.ZERO;
    }
}
