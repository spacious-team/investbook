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

package ru.investbook.parser.uralsib;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.ReportTable;
import ru.investbook.parser.SecurityTransaction;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.Security;
import ru.investbook.pojo.SecurityEventCashFlow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class DividendTable extends PaymentsTable {

    private static final String DIVIDEND_ACTION = "Доход по финансовым инструментам";

    public DividendTable(UralsibBrokerReport report,
                         SecuritiesTable securitiesTable,
                         ReportTable<SecurityTransaction> securityTransactionTable) {
        super(report, securitiesTable, securityTransactionTable);
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(Table table, TableRow row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        String description = table.getStringCellValue(row, DESCRIPTION);
        description = String.valueOf(description).toLowerCase();
        if (!action.equalsIgnoreCase(DIVIDEND_ACTION) || !description.contains("дивиденд")) {
            return emptyList();
        }

        Security security = getSecurity(table, row, CashFlowType.DIVIDEND);
        if (security == null) return emptyList();
        Instant timestamp = getReport().convertToInstant(table.getStringCellValue(row, DATE));

        BigDecimal tax = getTax(table, row);
        BigDecimal value = table.getCurrencyCellValue(row, VALUE)
                .add(tax.abs());
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .isin(security.getIsin())
                .portfolio(getReport().getPortfolio())
                .count(getSecurityCount(security, timestamp))
                .eventType(CashFlowType.DIVIDEND)
                .timestamp(timestamp)
                .value(value)
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)));

        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());

        if (tax.abs().compareTo(minValue) >= 0) {
            data.add(builder
                    .eventType(CashFlowType.TAX)
                    .value(tax.negate())
                    .build());
        }
        return data;
    }
}
