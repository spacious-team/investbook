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

package ru.investbook.parser.investbook;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.TableRow;

import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookPortfolioCashTable extends AbstractInvestbookTable<PortfolioCash> {

    protected InvestbookPortfolioCashTable(InvestbookBrokerReport report) {
        super(report);
    }

    @Override
    protected @Nullable PortfolioCash parseRow(TableRow row) {
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        if (!operation.contains("остаток")) { // Остаток денежных средств
            return null;
        }
        return PortfolioCash.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(parseEventInstant(row))
                .value(row.getBigDecimalCellValue(PRICE))
                .currency(row.getStringCellValue(CURRENCY))
                .market("")
                .build();
    }
}
