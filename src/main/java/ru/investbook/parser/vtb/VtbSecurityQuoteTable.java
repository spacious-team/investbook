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

package ru.investbook.parser.vtb;

import ru.investbook.parser.AbstractReportTable;
import ru.investbook.parser.BrokerReport;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.pojo.SecurityQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.*;

public class VtbSecurityQuoteTable extends AbstractReportTable<SecurityQuote> {

    public VtbSecurityQuoteTable(BrokerReport report) {
        super(report, VtbSecuritiesTable.TABLE_NAME, VtbSecuritiesTable.TABLE_FOOTER,
                VtbSecuritiesTable.VtbSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<SecurityQuote> getRow(Table table, TableRow row) {
        BigDecimal quote = table.getCurrencyCellValueOrDefault(row, QUOTE, null);
        if (quote == null) {
            return Collections.emptyList();
        }
        BigDecimal price = Optional.ofNullable(table.getCurrencyCellValueOrDefault(row, FACE_VALUE, null))
                .map(faceValue -> faceValue.multiply(quote).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .orElse(null);
        BigDecimal accruedInterest = table.getCurrencyCellValueOrDefault(row, ACCRUED_INTEREST, null);
        return Collections.singletonList(SecurityQuote.builder()
                .isin(table.getStringCellValue(row, NAME_AND_ISIN).split(",")[2].trim())
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .accruedInterest(accruedInterest)
                .build());
    }
}
