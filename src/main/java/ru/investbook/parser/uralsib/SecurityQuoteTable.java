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

package ru.investbook.parser.uralsib;

import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.pojo.SecurityQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.uralsib.SecuritiesTable.SecuritiesTableHeader.*;

public class SecurityQuoteTable extends AbstractReportTable<SecurityQuote> {

    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    protected SecurityQuoteTable(UralsibBrokerReport report) {
        super(report, SecuritiesTable.TABLE_NAME, SecuritiesTable.TABLE_END_TEXT,
                SecuritiesTable.SecuritiesTableHeader.class);
    }

    @Override
    protected Collection<SecurityQuote> getRow(Table table, TableRow row) {
        BigDecimal amount = table.getCurrencyCellValueOrDefault(row, AMOUNT, null);
        if (amount == null) {
            return Collections.emptyList();
        }
        int count = table.getIntCellValue(row, OUTGOING_COUNT);
        if (count == 0) {
            return Collections.emptyList();
        }
        BigDecimal price = amount.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        BigDecimal quote = table.getCurrencyCellValue(row, QUOTE);
        if (price.subtract(quote).abs().compareTo(minValue) < 0) {
            // котировка = цене (не облигация)
            price = null;
        }
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (accruedInterest.compareTo(minValue) < 0) {
            // не облигация
            accruedInterest = null;
        }
        return Collections.singletonList(SecurityQuote.builder()
                .isin(table.getStringCellValue(row, ISIN))
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .accruedInterest(accruedInterest)
                .build());
    }
}
