/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static java.util.Optional.ofNullable;
import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;
import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.*;

public class VtbSecurityQuoteTable extends SingleAbstractReportTable<SecurityQuote> {

    public VtbSecurityQuoteTable(SingleBrokerReport report) {
        super(report, VtbSecuritiesTable.TABLE_NAME, VtbSecuritiesTable.TABLE_FOOTER,
                VtbSecuritiesTable.VtbSecuritiesTableHeader.class);
    }

    @Override
    protected SecurityQuote parseRow(TableRow row) {
        BigDecimal quote = row.getBigDecimalCellValueOrDefault(QUOTE, null);
        if (quote == null) {
            return null;
        }
        BigDecimal price = ofNullable(row.getBigDecimalCellValueOrDefault(FACE_VALUE, null))
                .filter(faceValue -> faceValue.compareTo(minValue) > 0)
                .map(faceValue -> faceValue.multiply(quote).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
                .orElse(null);
        BigDecimal accruedInterest = null;
        if (price != null) {
            // имеет смысл только для облигаций, для акций price = null
            accruedInterest = ofNullable(row.getBigDecimalCellValueOrDefault(ACCRUED_INTEREST, null))
                    .filter(interest -> interest.compareTo(minValue) > 0) // otherwise outgoing count may be = 0
                    .map(interest -> {
                        int count = row.getIntCellValueOrDefault(OUTGOING, -1);
                        return (count <= 0) ? null : interest.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                    }).orElse(null);
        }
        String currency = ofNullable(row.getStringCellValueOrDefault(CURRENCY, null))
                .map(VtbBrokerReport::convertToCurrency)
                .orElse(null);
        return SecurityQuote.builder()
                .security(row.getStringCellValue(NAME_REGNUMBER_ISIN).split(",")[2].trim())
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .accruedInterest(accruedInterest)
                .currency(currency)
                .build();
    }
}
