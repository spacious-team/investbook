/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.pojo.PortfolioPropertyType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

public class PortfolioPropertyTable implements ReportTable<PortfolioProperty> {
    private static final String SUMMARY_TABLE = "Сводная информация по счетам клиента в валюте счета";
    private static final String ASSETS = "\"СУММА АКТИВОВ\" на конец дня";
    private static final String EXCHANGE_RATE_ROW = "Курс валют ЦБ РФ";
    private static final BigDecimal min = BigDecimal.valueOf(0.01);
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<PortfolioProperty> data = new ArrayList<>();


    protected PortfolioPropertyTable(PsbBrokerReport report) {
        this.report = report;
        ExcelTable table = getSummaryTable(report);
        this.data.addAll(getTotalAssets(table, report));
        this.data.addAll(getExchangeRate(table, report));
    }

    private static ExcelTable getSummaryTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), SUMMARY_TABLE, ASSETS, SummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }

    protected static Collection<PortfolioProperty> getTotalAssets(ExcelTable table, PsbBrokerReport report) {
        Row row = table.findRow(ASSETS);
        if (row == null) {
            return emptyList();
        }
        return Collections.singletonList(PortfolioProperty.builder()
                .portfolio(report.getPortfolio())
                .property(PortfolioPropertyType.TOTAL_ASSETS)
                .value(table.getCurrencyCellValue(row, RUB).toString())
                .timestamp(report.getReportDate())
                .build());
    }

    protected static Collection<PortfolioProperty> getExchangeRate(ExcelTable table, PsbBrokerReport report) {
        Row row = table.findRow(EXCHANGE_RATE_ROW);
        if (row == null) {
            return emptyList();
        }
        Collection<PortfolioProperty> rates = new ArrayList<>();
        rates.addAll(createExchangeRateProperty(report, table, row, USD, PortfolioPropertyType.USDRUB_EXCHANGE_RATE));
        rates.addAll(createExchangeRateProperty(report, table, row, EUR, PortfolioPropertyType.EURRUB_EXCHANGE_RATE));
        rates.addAll(createExchangeRateProperty(report, table, row, GBP, PortfolioPropertyType.GBPRUB_EXCHANGE_RATE));
        rates.addAll(createExchangeRateProperty(report, table, row, CHF, PortfolioPropertyType.CHFRUB_EXCHANGE_RATE));
        return rates;
    }

    private static Collection<PortfolioProperty> createExchangeRateProperty(PsbBrokerReport report, ExcelTable table,
                                                                            Row row, SummaryTableHeader currency,
                                                                            PortfolioPropertyType property) {
        BigDecimal exchangeRate = table.getCurrencyCellValue(row, currency);
        if (exchangeRate.compareTo(min) > 0) {
            return singletonList(PortfolioProperty.builder()
                    .portfolio(report.getPortfolio())
                    .property(property)
                    .value(exchangeRate.toString())
                    .timestamp(report.getReportDate())
                    .build());
        } else {
            return emptyList();
        }
    }

    enum SummaryTableHeader implements TableColumnDescription {
        DESCRIPTION(1),
        RUB("RUB"),
        USD("USD"),
        EUR("EUR"),
        GBP("GBP"),
        CHF("CHF");

        @Getter
        private final TableColumn column;
        SummaryTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }

        SummaryTableHeader(int columnIndex) {
            this.column = ConstantPositionTableColumn.of(columnIndex);
        }
    }
}
