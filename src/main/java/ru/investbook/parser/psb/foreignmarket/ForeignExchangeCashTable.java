/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;
import ru.investbook.parser.psb.PortfolioPropertyTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SUMMARY_TABLE;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;
import static ru.investbook.parser.psb.foreignmarket.ForeignExchangePortfolioPropertyTable.ASSETS;

@Slf4j
public class ForeignExchangeCashTable extends SingleInitializableReportTable<PortfolioCash> {

    private final PortfolioPropertyTable.SummaryTableHeader[] CURRENCIES = new PortfolioPropertyTable.SummaryTableHeader[]{ RUB, USD, EUR, GBP, CHF };

    public ForeignExchangeCashTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioCash> parseTable() {
        Table table = getSummaryTable();
        @Nullable TableRow row = table.findRowByPrefix(ASSETS);
        if (row == null) {
            return emptyList();
        }
        Collection<PortfolioCash> cashes = new ArrayList<>();
        for (PortfolioPropertyTable.SummaryTableHeader currency : CURRENCIES) {
            @Nullable BigDecimal cash = row.getBigDecimalCellValueOrDefault(currency, null);
            if (cash != null) {
                cashes.add(PortfolioCash.builder()
                        .portfolio(getReport().getPortfolio())
                        .timestamp(getReport().getReportEndDateTime())
                        .market("валютный рынок")
                        .value(cash)
                        .currency(currency.name())
                        .build());
            }
        }
        return cashes;
    }

    private Table getSummaryTable() {
        Table table = getReport().getReportPage()
                .create(SUMMARY_TABLE, ASSETS, PortfolioPropertyTable.SummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }
}
