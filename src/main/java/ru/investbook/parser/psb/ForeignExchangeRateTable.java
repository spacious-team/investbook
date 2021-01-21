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

package ru.investbook.parser.psb;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.CurrencyPair;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

@Slf4j
public class ForeignExchangeRateTable extends InitializableReportTable<ForeignExchangeRate> {
    public static final String EXCHANGE_RATE_ROW = "Курс валют ЦБ РФ";
    private final BigDecimal min = BigDecimal.valueOf(0.01);

    public ForeignExchangeRateTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<ForeignExchangeRate> parseTable() {
        Table table = PortfolioPropertyTable.getSummaryTable(getReport(), PortfolioPropertyTable.ASSETS);
        return getExchangeRate(table);
    }

    protected Collection<ForeignExchangeRate> getExchangeRate(Table table) {
        try {
            TableRow row = table.findRow(EXCHANGE_RATE_ROW);
            if (row == null) {
                return emptyList();
            }
            Collection<ForeignExchangeRate> rates = new ArrayList<>();
            rates.addAll(createExchangeRateProperty(table, row, USD, CurrencyPair.USDRUB));
            rates.addAll(createExchangeRateProperty(table, row, EUR, CurrencyPair.EURRUB));
            rates.addAll(createExchangeRateProperty(table, row, GBP, CurrencyPair.GBPRUB));
            rates.addAll(createExchangeRateProperty(table, row, CHF, CurrencyPair.CHFRUB));
            return rates;
        } catch (Exception e) {
            log.info("Ошибка поиска стоимости активов или обменного курса в файле {}", getReport().getPath().getFileName(), e);
            return emptyList();
        }
    }

    private Collection<ForeignExchangeRate> createExchangeRateProperty(Table table,
                                                                     TableRow row, PortfolioPropertyTable.SummaryTableHeader currency,
                                                                     CurrencyPair currencyPair) {
        BigDecimal exchangeRate = table.getCurrencyCellValueOrDefault(row, currency, BigDecimal.ZERO);
        if (exchangeRate.compareTo(min) > 0) {
            return singletonList(ForeignExchangeRate.builder()
                    .date(LocalDate.ofInstant(getReport().getReportEndDateTime(), getReport().getReportZoneId()))
                    .currencyPair(currencyPair.name())
                    .rate(exchangeRate)
                    .build());
        } else {
            return emptyList();
        }
    }
}
