/*
 * InvestBook
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.*;
import ru.investbook.parser.table.TableCell;
import ru.investbook.parser.table.TableCellAddress;
import ru.investbook.parser.table.TableRow;
import ru.investbook.parser.table.excel.ExcelTable;
import ru.investbook.pojo.PortfolioProperty;
import ru.investbook.pojo.PortfolioPropertyType;
import ru.investbook.view.ForeignExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.Double.parseDouble;
import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.PortfolioPropertyTable.SummaryTableHeader.RUB;

@Slf4j
public class PortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {
    private static final String ASSETS_TABLE = "ОЦЕНКА АКТИВОВ";
    private static final String TABLE_FIRST_HEADER_LINE = "На конец отчетного периода";
    private static final String TABLE_SECOND_HEADER_LINE = "по цене закрытия";
    private static final String ASSETS = "Общая стоимость активов:";
    private static final String EXCHANGE_RATE = "Официальный обменный курс";
    private final ForeignExchangeRateService foreignExchangeRateService;

    protected PortfolioPropertyTable(UralsibBrokerReport report, ForeignExchangeRateService foreignExchangeRateService) {
        super(report);
        this.foreignExchangeRateService = foreignExchangeRateService;
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        List<PortfolioProperty> data = new ArrayList<>();
        data.addAll(getTotalAssets(getReport()));
        data.addAll(getExchangeRate(getReport()));
        return data;
    }

    protected static Collection<PortfolioProperty> getTotalAssets(BrokerReport report) {
        try {
            ExcelTable table = ExcelTable.ofNoName(report.getReportPage(), ASSETS_TABLE, TABLE_FIRST_HEADER_LINE,
                    SummaryTableHeader.class, 3);
            if (table.isEmpty()) {
                table = ExcelTable.ofNoName(report.getReportPage(), ASSETS_TABLE, TABLE_SECOND_HEADER_LINE,
                        SummaryTableHeader.class, 2);
            }
            if (table.isEmpty()) {
                log.info("Таблица '{}' не найдена", ASSETS_TABLE);
                return emptyList();
            }
            TableRow row = table.findRow(ASSETS);
            if (row == null) {
                return emptyList();
            }
            return Collections.singletonList(PortfolioProperty.builder()
                    .portfolio(report.getPortfolio())
                    .property(PortfolioPropertyType.TOTAL_ASSETS)
                    .value(table.getCurrencyCellValue(row, RUB).toString())
                    .timestamp(report.getReportDate())
                    .build());
        } catch (Exception e) {
            log.info("Не могу распарсить таблицу '{}' в файле {}", ASSETS_TABLE, report.getPath().getFileName(), e);
            return emptyList();
        }
    }

    protected static Collection<PortfolioProperty> getExchangeRate(BrokerReport report) {
        try {
            TableCellAddress address = report.getReportPage().find(EXCHANGE_RATE);
            if (address == TableCellAddress.NOT_FOUND) {
                return emptyList();
            }
            List<PortfolioProperty> exchangeRates = new ArrayList<>();
            TableCell cell = report.getReportPage().getRow(address.getRow() + 1).getCell(0);
            String text = report.getReportPage().getStringCellValue(cell);
            String[] words = text.split(" ");
            for (int i = 0; i < words.length; i++) {
                try {
                    String word = words[i];
                    if (word.equalsIgnoreCase("=")) {
                        String currency = words[i - 1];
                        BigDecimal exchangeRate = BigDecimal.valueOf(parseDouble(words[i + 1].replace(",", ".")));
                        exchangeRates.add(PortfolioProperty.builder()
                                .portfolio(report.getPortfolio())
                                .property(ForeignExchangeRateService.getExchangePropertyFor(currency))
                                .value(exchangeRate.toString())
                                .timestamp(report.getReportDate())
                                .build());
                    }
                } catch (Exception e) {
                    log.debug("Не смог распарсить курс валюты из отчета", e);
                }
            }
            return exchangeRates;
        } catch (Exception e) {
            log.debug("Не могу найти обменный курс в файле {}", report.getPath().getFileName(), e);
            return emptyList();
        }
    }

    /**
     * Returns foreign exchange rate to given transaction time instant from report or from database.
     */
    public BigDecimal getExchangeRate(String baseCurrency, String quoteCurrency, Instant transactionInstant) {
        if (baseCurrency.equalsIgnoreCase(quoteCurrency)) {
            return BigDecimal.ONE;
        }
        BigDecimal exchangeRate = BigDecimal.ZERO;
        if (quoteCurrency.equalsIgnoreCase("rub")) {
            exchangeRate = getReportExchangeRate(baseCurrency);
        } else if (baseCurrency.equalsIgnoreCase("rub")) {
            BigDecimal v = getReportExchangeRate(quoteCurrency);
            exchangeRate = v.equals(BigDecimal.ZERO) ? v : BigDecimal.ONE.divide(v, 6, RoundingMode.HALF_UP);
        } else {
            BigDecimal baseToRubRate = getReportExchangeRate(baseCurrency);
            BigDecimal quoteToRubRate = getReportExchangeRate(quoteCurrency);
            if (!baseToRubRate.equals(BigDecimal.ZERO) && !quoteToRubRate.equals(BigDecimal.ZERO)) {
                exchangeRate = baseToRubRate.divide(quoteToRubRate, 6, RoundingMode.HALF_UP);
            }
        }
        if (exchangeRate.equals(BigDecimal.ZERO)) {
            exchangeRate = foreignExchangeRateService.getExchangeRate(baseCurrency, quoteCurrency,
                    transactionInstant, UralsibBrokerReport.zoneId);
        }
        return exchangeRate;
    }

    private BigDecimal getReportExchangeRate(String currency) {
        try {
            return getData().stream()
                    .filter(e -> e.getProperty().equals(ForeignExchangeRateService.getExchangePropertyFor(currency)))
                    .map(PortfolioProperty::getValue)
                    .map(s -> BigDecimal.valueOf(Double.parseDouble(s)))
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @RequiredArgsConstructor
    enum SummaryTableHeader implements TableColumnDescription {
        RUB(AnyOfTableColumn.of(
                MultiLineTableColumn.of(
                        TableColumnImpl.of(TABLE_FIRST_HEADER_LINE),
                        TableColumnImpl.of(TABLE_SECOND_HEADER_LINE),
                        TableColumnImpl.of("RUR")),
                MultiLineTableColumn.of(
                        TableColumnImpl.of(TABLE_SECOND_HEADER_LINE),
                        TableColumnImpl.of("RUR"))));

        @Getter
        private final TableColumn column;
    }
}
