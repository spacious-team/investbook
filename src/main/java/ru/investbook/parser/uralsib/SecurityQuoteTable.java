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
    private final BigDecimal HUNDRED = BigDecimal.valueOf(100);

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
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST); // в валюте для валютных облигаций
        String isin = row.getStringCellValue(ISIN);
        Instant reportEndDateTime = getReport().getReportEndDateTime();

        boolean isShare = false;
        BigDecimal price = null;
        String quoteCurrency = null;

        if (accruedInterest.compareTo(minValue) < 0) { // акция или облигация с НКД == 0 ?
            if (priceInRub.subtract(quote).abs().compareTo(minValue) < 0) {
                // акция, котировка в руб
                isShare = true;
                quoteCurrency = "RUB";
                price = null;
                accruedInterest = null;
            } else {
                for (String currency : currencies) {
                    try {
                        BigDecimal rate = foreignExchangeRateTable.getExchangeRate(currency, "RUB", reportEndDateTime);
                        if (priceInRub.divide(rate, 2, HALF_UP).subtract(quote).abs().compareTo(minValue) < 0) {
                            // акция (Tesla, Apple), котировка в иностранной валюте
                            isShare = true;
                            quoteCurrency = currency;
                            price = null;
                            accruedInterest = null;
                            break;
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }

        if (!isShare) {
            // Облигация. Цена в отчете в рублях, НКД может быть в валюте
            BigDecimal totalFaceValueInRub = amountInRub.multiply(HUNDRED).divide(quote, 2, HALF_UP); // номинал в рублях
            boolean isRubBond = false;
            try {
                int exactTotalFaceValueInRub = totalFaceValueInRub.intValueExact();
                if (exactTotalFaceValueInRub % 1000 == 0 || exactTotalFaceValueInRub % 100 == 0) {
                    // скорее всего рублевая облигация
                    isRubBond = true;
                    quoteCurrency = "RUB";
                    price = priceInRub;
                    // НКД уже в рублях
                }
            } catch (Exception ignore) {
            }

            if (!isRubBond) {
                for (String currency : currencies) {
                    try {
                        BigDecimal rate = foreignExchangeRateTable.getExchangeRate(currency, "RUB", reportEndDateTime);
                        totalFaceValueInRub.divide(rate, 2, HALF_UP).intValueExact();
                        // облигация в иностранной валюте, приводим цену к валюте
                        quoteCurrency = currency;
                        price = priceInRub.divide(rate, 4, HALF_UP);
                        // НКД уже в валюте
                        break;
                    } catch (Exception ignore) {
                    }
                }
            }

            if (quoteCurrency == null && accruedInterest.floatValue() > 0.01) {
                throw new IllegalArgumentException("Не смогли вычислить валюту облигации " + isin +
                        " , цена и НКД могут быть в разных валютах");
            }
        }
        return SecurityQuote.builder()
                .security(isin)
                .timestamp(reportEndDateTime)
                .quote(quote)
                .price(price)
                .accruedInterest(accruedInterest)
                .currency(quoteCurrency)
                .build();
    }
}
