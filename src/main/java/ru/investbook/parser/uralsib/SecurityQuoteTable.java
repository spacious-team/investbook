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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityQuote.SecurityQuoteBuilder;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static ru.investbook.parser.uralsib.SecuritiesTable.SecuritiesTableHeader.*;
import static ru.investbook.parser.uralsib.SecurityRegistryHelper.declareStockOrBond;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Slf4j
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

        Optional<SecurityQuoteBuilder> builder = getShareQuote(quote, priceInRub, accruedInterest)
                .or(() -> getBondQuote(quote, priceInRub, accruedInterest, amountInRub));
        if (builder.isEmpty()) {
            log.debug("Не смогли вычислить валюту облигации {}, цена и НКД могут быть в разных валютах", isin);
            return null;
        }
        int securityId = declareStockOrBond(isin, row.getStringCellValue(NAME), getReport().getSecurityRegistrar());

        return builder.get()
                .security(securityId)
                .timestamp(getReport().getReportEndDateTime())
                .build();
    }

    private Optional<SecurityQuoteBuilder> getShareQuote(BigDecimal quote, BigDecimal priceInRub, BigDecimal accruedInterest) {
        if (accruedInterest.compareTo(minValue) < 0) { // акция или облигация с НКД == 0
            if (priceInRub.subtract(quote).abs().compareTo(minValue) < 0) {
                // акция, котировка в руб
                return of(SecurityQuote.builder()
                        .quote(quote)
                        .currency(RUB));
            } else {
                Instant reportEndDateTime = getReport().getReportEndDateTime();
                for (String tryCurrency : currencies) {
                    try {
                        BigDecimal rate = foreignExchangeRateTable
                                .getExchangeRate(tryCurrency, RUB, reportEndDateTime);
                        if (priceInRub.divide(rate, 2, HALF_UP).subtract(quote).abs().compareTo(minValue) < 0) {
                            // акция (Tesla, Apple), котировка в иностранной валюте
                            return of(SecurityQuote.builder()
                                    .quote(quote)
                                    .currency(tryCurrency));
                        }
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        return empty();
    }

    private Optional<SecurityQuoteBuilder> getBondQuote(BigDecimal quote, BigDecimal priceInRub, BigDecimal accruedInterest,
                                                        BigDecimal amountInRub) {
        // Цена в отчете в рублях, НКД может быть в валюте, если так, надо привести цену к валюте
        BigDecimal totalFaceValueInRub = amountInRub.multiply(HUNDRED).divide(quote, 2, HALF_UP); // номинал в рублях
        try {
            int exactTotalFaceValueInRub = totalFaceValueInRub.intValueExact();
            if (exactTotalFaceValueInRub % 1000 == 0 || exactTotalFaceValueInRub % 100 == 0) {
                // скорее всего рублевая облигация
                return of(SecurityQuote.builder()
                        .quote(quote)
                        .price(priceInRub)
                        .accruedInterest(accruedInterest)
                        .currency(RUB));
            }
        } catch (Exception ignore) {
        }

        Instant reportEndDateTime = getReport().getReportEndDateTime();
        for (String tryCurrency : currencies) {
            try {
                BigDecimal rate = foreignExchangeRateTable
                        .getExchangeRate(tryCurrency, RUB, reportEndDateTime);
                totalFaceValueInRub.divide(rate, 2, HALF_UP).intValueExact();
                // не было исключения, облигация в иностранной валюте, приводим цену к валюте
                return of(SecurityQuote.builder()
                        .quote(quote)
                        .price(priceInRub.divide(rate, 4, HALF_UP))
                        .accruedInterest(accruedInterest)
                        .currency(tryCurrency));
            } catch (Exception ignore) {
            }
        }
        return empty();
    }
}
