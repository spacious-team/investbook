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

package ru.investbook.parser.psb;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.*;

public class SecurityQuoteTable extends SingleAbstractReportTable<SecurityQuote> {

    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public SecurityQuoteTable(PsbBrokerReport report) {
        super(report, SecuritiesTable.TABLE_NAME, SecuritiesTable.TABLE_END_TEXT,
                SecuritiesTable.SecuritiesTableHeader.class);
    }

    @Override
    protected SecurityQuote parseRow(TableRow row) {
        if (row.rowContains(SecuritiesTable.INVALID_TEXT)) {
            return null;
        }
        int count = row.getIntCellValue(OUTGOING);
        if (count == 0) {
            return null;
        }
        String isin = row.getStringCellValue(ISIN);
        BigDecimal amount = row.getBigDecimalCellValue(AMOUNT);
        BigDecimal price = amount.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP);
        BigDecimal quote = row.getBigDecimalCellValue(QUOTE);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        String currency = row.getStringCellValue(CURRENCY);
        if (accruedInterest.compareTo(minValue) < 0 && price.subtract(quote).abs().compareTo(minValue) < 0) {
            // акция
            price = null;
            accruedInterest = null;
        } else {
            // облигация
            String accruedInterestCurrency = row.getStringCellValue(FACEUNIT);
            if (!currency.equals(accruedInterestCurrency)) {
                throw new UnsupportedOperationException("ISIN: " + isin + ". " +
                        "Валюта купона и оценка стоимости в отчете брокера в разных валютах." +
                        " Не могу привести валюту купона к валюте цены, не реализовано.");
            }
        }
        String securityId = getReport().getSecurityRegistrar().declareStockOrBond(isin, () -> Security.builder()
                .id(isin)
                .isin(isin)
                .name(row.getStringCellValue(NAME)));
        return SecurityQuote.builder()
                .security(securityId)
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .accruedInterest(accruedInterest)
                .currency(currency)
                .build();
    }
}
