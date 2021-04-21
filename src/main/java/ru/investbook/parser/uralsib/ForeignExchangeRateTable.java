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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.table_wrapper.api.TableCell;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;
import ru.investbook.report.ForeignExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.util.Collections.emptyList;

@Slf4j
public class ForeignExchangeRateTable extends SingleInitializableReportTable<ForeignExchangeRate> {
    private static final String EXCHANGE_RATE = "Официальный обменный курс";
    private final ForeignExchangeRateService foreignExchangeRateService;

    protected ForeignExchangeRateTable(UralsibBrokerReport report, ForeignExchangeRateService foreignExchangeRateService) {
        super(report);
        this.foreignExchangeRateService = foreignExchangeRateService;
    }

    @Override
    protected Collection<ForeignExchangeRate> parseTable() {
        try {
            SingleBrokerReport report = getReport();
            TableCellAddress address = report.getReportPage().find(EXCHANGE_RATE);
            if (address == TableCellAddress.NOT_FOUND) {
                return emptyList();
            }
            List<ForeignExchangeRate> exchangeRates = new ArrayList<>();
            TableCell cell = report.getReportPage().getRow(address.getRow() + 1).getCell(0);
            String text = cell.getStringValue();
            String[] words = text.split(" ");
            for (int i = 0; i < words.length; i++) {
                try {
                    String word = words[i];
                    if (word.equalsIgnoreCase("=")) {
                        String currency = words[i - 1];
                        BigDecimal exchangeRate = BigDecimal.valueOf(parseDouble(words[i + 1].replace(",", ".")));
                        exchangeRates.add(ForeignExchangeRate.builder()
                                .date(LocalDate.ofInstant(report.getReportEndDateTime(), report.getReportZoneId()))
                                .currencyPair(currency + "RUB")
                                .rate(exchangeRate)
                                .build());
                    }
                } catch (Exception e) {
                    log.debug("Не смог распарсить курс валюты из отчета", e);
                }
            }
            return exchangeRates;
        } catch (Exception e) {
            log.debug("Не могу найти обменный курс в файле {}", getReport().getPath().getFileName(), e);
            return emptyList();
        }
    }

    /**
     * Returns foreign exchange rate to given transaction time instant from report or from database.
     */
    BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, Instant transactionInstant) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = BigDecimal.ZERO;
        LocalDate atDate = LocalDate.ofInstant(transactionInstant, getReport().getReportZoneId());
        if (quoteCurrency.equalsIgnoreCase("rub")) {
            exchangeRate = getReportExchangeRate(baseCurrency, atDate);
        } else if (baseCurrency.equalsIgnoreCase("rub")) {
            BigDecimal v = getReportExchangeRate(quoteCurrency, atDate);
            exchangeRate = v.equals(BigDecimal.ZERO) ? v : BigDecimal.ONE.divide(v, 6, RoundingMode.HALF_UP);
        } else {
            BigDecimal baseToRubRate = getReportExchangeRate(baseCurrency, atDate);
            BigDecimal quoteToRubRate = getReportExchangeRate(quoteCurrency, atDate);
            if (!baseToRubRate.equals(BigDecimal.ZERO) && !quoteToRubRate.equals(BigDecimal.ZERO)) {
                exchangeRate = baseToRubRate.divide(quoteToRubRate, 6, RoundingMode.HALF_UP);
            }
        }
        if (exchangeRate.equals(BigDecimal.ZERO)) {
            exchangeRate = foreignExchangeRateService.getExchangeRate(baseCurrency, quoteCurrency, atDate);
        }
        return exchangeRate;
    }

    private BigDecimal getReportExchangeRate(String currency, LocalDate atDate) {
        try {
            return getData().stream()
                    .filter(e -> e.getDate().equals(atDate))
                    .filter(e -> e.getCurrencyPair().equals(currency + "RUB"))
                    .map(ForeignExchangeRate::getRate)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
