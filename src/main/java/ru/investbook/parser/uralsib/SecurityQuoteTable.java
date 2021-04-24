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

package ru.investbook.parser.uralsib;

import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static java.math.RoundingMode.HALF_UP;
import static ru.investbook.parser.uralsib.SecuritiesTable.SecuritiesTableHeader.*;

public class SecurityQuoteTable extends SingleAbstractReportTable<SecurityQuote> {

    private final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final ForeignExchangeRateTable foreignExchangeRateTable;
    private final Collection<String> currencies = List.of("USD", "EUR", "GBP");

    protected SecurityQuoteTable(UralsibBrokerReport report, ForeignExchangeRateTable foreignExchangeRateTable) {
        super(report, SecuritiesTable.TABLE_NAME, SecuritiesTable.TABLE_END_TEXT,
                SecuritiesTable.SecuritiesTableHeader.class);
        this.foreignExchangeRateTable = foreignExchangeRateTable;
    }

    @Override
    protected SecurityQuote parseRow(TableRow row) {
        BigDecimal amountInRub = row.getBigDecimalCellValueOrDefault(AMOUNT, null);
        if (amountInRub == null || amountInRub.compareTo(minValue) < 0) {
            return null;
        }
        int count = row.getIntCellValue(OUTGOING_COUNT);
        if (count == 0) {
            return null;
        }
        BigDecimal priceInRub = amountInRub.divide(BigDecimal.valueOf(count), 4, HALF_UP);
        BigDecimal quote = row.getBigDecimalCellValue(QUOTE);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST);
        String isin = row.getStringCellValue(ISIN);
        Instant reportEndDateTime = getReport().getReportEndDateTime();
        if (accruedInterest.compareTo(minValue) < 0) { // акция или облигация с НКД == 0 ?
            if (priceInRub.subtract(quote).abs().compareTo(minValue) < 0) {
                // акция, котировка в руб
                priceInRub = null;
                accruedInterest = null;
            } else {
                for (String currency : currencies) {
                    try {
                        BigDecimal usdRub = foreignExchangeRateTable.getExchangeRate(currency, "RUB", reportEndDateTime);
                        if (priceInRub.divide(usdRub, 2, HALF_UP).subtract(quote).abs().compareTo(minValue) < 0) {
                            // акция (Tesla, Apple), котировка в иностранной валюте
                            priceInRub = null;
                            accruedInterest = null;
                            break;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        return SecurityQuote.builder()
                .security(isin)
                .timestamp(reportEndDateTime)
                .quote(quote)
                .price(priceInRub)
                .accruedInterest(accruedInterest)
                .build();
    }
}
