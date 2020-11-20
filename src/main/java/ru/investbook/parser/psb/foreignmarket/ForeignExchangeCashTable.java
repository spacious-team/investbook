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

package ru.investbook.parser.psb.foreignmarket;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.spacious_team.broker.report_parser.api.TableFactoryRegistry;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableFactory;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.psb.PortfolioPropertyTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SUMMARY_TABLE;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;
import static ru.investbook.parser.psb.foreignmarket.ForeignExchangePortfolioPropertyTable.ASSETS;

@Slf4j
public class ForeignExchangeCashTable extends InitializableReportTable<PortfolioCash> {

    private final PortfolioPropertyTable.SummaryTableHeader[] CURRENCIES = new PortfolioPropertyTable.SummaryTableHeader[]{ RUB, USD, EUR, GBP, CHF };

    public ForeignExchangeCashTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioCash> parseTable() {
        Table table = getSummaryTable();
        TableRow row = table.findRow(ASSETS);
        if (row == null) {
            return emptyList();
        }
        Collection<PortfolioCash> cashes = new ArrayList<>();
        for (PortfolioPropertyTable.SummaryTableHeader currency : CURRENCIES) {
            BigDecimal cash = table.getCurrencyCellValueOrDefault(row, currency, null);
            if (cash != null) {
                cashes.add(PortfolioCash.builder()
                        .section("валютный рынок")
                        .value(cash)
                        .currency(currency.name())
                        .build());
            }
        }
        return cashes;
    }

    private Table getSummaryTable() {
        ReportPage reportPage = getReport().getReportPage();
        TableFactory tableFactory = TableFactoryRegistry.get(reportPage);
        Table table = tableFactory.create(reportPage, SUMMARY_TABLE, ASSETS, PortfolioPropertyTable.SummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }
}
