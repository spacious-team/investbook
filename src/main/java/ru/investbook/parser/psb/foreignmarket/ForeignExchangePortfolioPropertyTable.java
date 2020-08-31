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

package ru.investbook.parser.psb.foreignmarket;

import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.BrokerReport;
import ru.investbook.parser.psb.PortfolioPropertyTable;
import ru.investbook.parser.table.*;
import ru.investbook.pojo.PortfolioProperty;
import ru.investbook.pojo.PortfolioPropertyType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

@Slf4j
public class ForeignExchangePortfolioPropertyTable extends PortfolioPropertyTable {

    static final String ASSETS = "Остаток средств на счете";

    public ForeignExchangePortfolioPropertyTable(BrokerReport report) {
        super(report);
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

    @Override
    protected Collection<PortfolioProperty> getTotalAssets(Table table) {
        try {
            TableRow assetsRow = table.findRow(ASSETS);
            TableRow exchangeRateRow = table.findRow(EXCHANGE_RATE_ROW);
            if (assetsRow == null || exchangeRateRow == null) {
                return emptyList();
            }
            BigDecimal totalAssets = table.getCurrencyCellValueOrDefault(assetsRow, RUB, BigDecimal.ZERO)
                    .add(table.getCurrencyCellValueOrDefault(assetsRow, USD, BigDecimal.ZERO)
                            .multiply(table.getCurrencyCellValueOrDefault(exchangeRateRow, USD, BigDecimal.ZERO)))
                    .add(table.getCurrencyCellValueOrDefault(assetsRow, EUR, BigDecimal.ZERO)
                            .multiply(table.getCurrencyCellValueOrDefault(exchangeRateRow, EUR, BigDecimal.ZERO)))
                    .add(table.getCurrencyCellValueOrDefault(assetsRow, GBP, BigDecimal.ZERO)
                            .multiply(table.getCurrencyCellValueOrDefault(exchangeRateRow, GBP, BigDecimal.ZERO)))
                    .add(table.getCurrencyCellValueOrDefault(assetsRow, CHF, BigDecimal.ZERO)
                            .multiply(table.getCurrencyCellValueOrDefault(exchangeRateRow, CHF, BigDecimal.ZERO)));
            if (totalAssets.compareTo(min) > 0) {
                return Collections.singletonList(PortfolioProperty.builder()
                        .portfolio(getReport().getPortfolio())
                        .property(PortfolioPropertyType.TOTAL_ASSETS)
                        .value(totalAssets.toString())
                        .timestamp(getReport().getReportDate())
                        .build());
            }
        } catch (Exception e) {
            log.info("Не могу получить стоимость активов из отчета {}", getReport().getPath().getFileName());
        }
        return emptyList();
    }
}
