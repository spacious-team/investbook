/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;
import org.spacious_team.broker.report_parser.api.TableFactoryRegistry;
import org.spacious_team.table_wrapper.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

@Slf4j
public class PortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {
    public static final String SUMMARY_TABLE = "Сводная информация по счетам клиента в валюте счета";
    private static final String ASSETS = "\"СУММА АКТИВОВ\" на конец дня";
    public static final String EXCHANGE_RATE_ROW = "Курс валют ЦБ РФ";
    private final BigDecimal min = BigDecimal.valueOf(0.01);

    public PortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        Table table = getSummaryTable();
        Collection<PortfolioProperty> data = new ArrayList<>();
        data.addAll(getTotalAssets(table));
        data.addAll(getExchangeRate(table));
        return data;
    }

    protected Table getSummaryTable() {
        ReportPage reportPage = getReport().getReportPage();
        TableFactory tableFactory = TableFactoryRegistry.get(reportPage);
        Table table = tableFactory.create(reportPage, SUMMARY_TABLE, ASSETS, SummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }

    protected Collection<PortfolioProperty> getTotalAssets(Table table) {
        try {
            TableRow row = table.findRow(ASSETS);
            if (row == null) {
                return emptyList();
            }
            return Collections.singletonList(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .property(PortfolioPropertyType.TOTAL_ASSETS)
                    .value(table.getCurrencyCellValue(row, RUB).toString())
                    .timestamp(getReport().getReportEndDateTime())
                    .build());
        } catch (Exception e) {
            log.info("Не могу получить стоимость активов из отчета {}", getReport().getPath().getFileName());
            return emptyList();
        }
    }

    protected Collection<PortfolioProperty> getExchangeRate(Table table) {
        try {
            TableRow row = table.findRow(EXCHANGE_RATE_ROW);
            if (row == null) {
                return emptyList();
            }
            Collection<PortfolioProperty> rates = new ArrayList<>();
            rates.addAll(createExchangeRateProperty(table, row, USD, PortfolioPropertyType.USDRUB_EXCHANGE_RATE));
            rates.addAll(createExchangeRateProperty(table, row, EUR, PortfolioPropertyType.EURRUB_EXCHANGE_RATE));
            rates.addAll(createExchangeRateProperty(table, row, GBP, PortfolioPropertyType.GBPRUB_EXCHANGE_RATE));
            rates.addAll(createExchangeRateProperty(table, row, CHF, PortfolioPropertyType.CHFRUB_EXCHANGE_RATE));
            return rates;
        } catch (Exception e) {
            log.info("Ошибка поиска стоимости активов или обменного курса в файле {}", getReport().getPath().getFileName(), e);
            return emptyList();
        }
    }

    private Collection<PortfolioProperty> createExchangeRateProperty(Table table,
                                                                     TableRow row, SummaryTableHeader currency,
                                                                     PortfolioPropertyType property) {
        BigDecimal exchangeRate = table.getCurrencyCellValueOrDefault(row, currency, BigDecimal.ZERO);
        if (exchangeRate.compareTo(min) > 0) {
            return singletonList(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .property(property)
                    .value(exchangeRate.toString())
                    .timestamp(getReport().getReportEndDateTime())
                    .build());
        } else {
            return emptyList();
        }
    }

    @RequiredArgsConstructor
    public enum SummaryTableHeader implements TableColumnDescription {
        DESCRIPTION(1),
        RUB("RUB"),
        USD(OptionalTableColumn.of(TableColumnImpl.of("USD"))),
        EUR(OptionalTableColumn.of(TableColumnImpl.of("EUR"))),
        GBP(OptionalTableColumn.of(TableColumnImpl.of("GBP"))),
        CHF(OptionalTableColumn.of(TableColumnImpl.of("CHF")));

        @Getter
        private final TableColumn column;
        SummaryTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }

        SummaryTableHeader(int columnIndex) {
            this.column = ConstantPositionTableColumn.of(columnIndex);
        }
    }
}
