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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import static ru.investbook.parser.psb.CashTable.CashTableHeader.*;

@Slf4j
public class CashTable extends SingleAbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "Позиция денежных средств по биржевым площадкам";
    private static final String TABLE_END_TEXT = "КонецДС_Б"; // hidden text in 0-th column
    private static final String INVALID_TEXT = "ИТОГО:";

    public CashTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, CashTableHeader.class, 2);
    }

    @Override
    protected @Nullable PortfolioCash parseRow(TableRow row) {
        return row.rowContains(INVALID_TEXT) ? null :
                PortfolioCash.builder()
                        .portfolio(getReport().getPortfolio())
                        .timestamp(getReport().getReportEndDateTime())
                        .market(row.getStringCellValue(SECTION))
                        .value(row.getBigDecimalCellValue(VALUE))
                        .currency(row.getStringCellValue(CURRENCY))
                        .build();
    }

    @Getter
    enum CashTableHeader implements TableHeaderColumn {
        SECTION("сектор"),
        VALUE("плановый исходящий остаток"),
        CURRENCY("валюта");

        private final TableColumn column;

        CashTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
