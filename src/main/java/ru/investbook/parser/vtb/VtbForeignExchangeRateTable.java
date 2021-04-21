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

package ru.investbook.parser.vtb;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import ru.investbook.parser.CurrencyPair;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import static java.lang.Double.parseDouble;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Slf4j
public class VtbForeignExchangeRateTable extends SingleInitializableReportTable<ForeignExchangeRate> {

    private static final String USD_EXCHANGE_RATE = "Курс USD (по курсу ЦБ на конечную дату отчета)";
    private static final String EUR_EXCHANGE_RATE = "Курс EUR (по курсу ЦБ на конечную дату отчета)";
    private static final String CHF_EXCHANGE_RATE = "Курс CHF (по курсу ЦБ на конечную дату отчета)";
    private static final String GBP_EXCHANGE_RATE = "Курс GBP (по курсу ЦБ на конечную дату отчета)";

    public VtbForeignExchangeRateTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<ForeignExchangeRate> parseTable() {
        Collection<ForeignExchangeRate> data = new ArrayList<>();
        data.addAll(buildPortfolioProperty(
                CurrencyPair.USDRUB,
                USD_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                CurrencyPair.EURRUB,
                EUR_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                CurrencyPair.CHFRUB,
                CHF_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                CurrencyPair.GBPRUB,
                GBP_EXCHANGE_RATE));
        return data;
    }

    private Collection<ForeignExchangeRate> buildPortfolioProperty(CurrencyPair currencyPair, String rowHeader) {
        try {
            String value = getReport().getReportPage()
                    .getNextColumnValue(rowHeader)
                    .toString()
                    .replace(',', '.');
            BigDecimal rate = BigDecimal.valueOf(parseDouble(value));
            return singleton(ForeignExchangeRate.builder()
                    .date(LocalDate.ofInstant(getReport().getReportEndDateTime(), getReport().getReportZoneId()))
                    .currencyPair(currencyPair.name())
                    .rate(rate)
                    .build());
        } catch (Exception e) {
            log.debug("Не удалось распарсить '{}' из {}", rowHeader, getReport().getPath());
            return emptyList();
        }
    }
}
