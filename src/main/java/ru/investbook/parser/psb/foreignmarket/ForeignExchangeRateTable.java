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

package ru.investbook.parser.psb.foreignmarket;

import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.table_wrapper.api.Table;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.psb.PortfolioPropertyTable;

import java.util.Collection;

public class ForeignExchangeRateTable extends ru.investbook.parser.psb.ForeignExchangeRateTable {

    public ForeignExchangeRateTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<ForeignExchangeRate> parseTable() {
        Table table = PortfolioPropertyTable.getSummaryTable(getReport(), ForeignExchangePortfolioPropertyTable.ASSETS);
        return getExchangeRate(table);
    }
}
