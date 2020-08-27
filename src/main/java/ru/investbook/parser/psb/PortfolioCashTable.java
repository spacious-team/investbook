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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.*;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.psb.PortfolioCashTable.CashTableHeader.*;

@Slf4j
public class PortfolioCashTable extends AbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "Позиция денежных средств по биржевым площадкам";
    private static final String TABLE_END_TEXT = "КонецДС_Б"; // hidden text in 0-th column
    private static final String INVALID_TEXT = "ИТОГО:";

    public PortfolioCashTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, CashTableHeader.class, 2);
    }

    @Override
    protected Collection<PortfolioCash> getRow(Table table, TableRow row) {
        return row.rowContains(INVALID_TEXT) ?
                emptyList() :
                singletonList(PortfolioCash.builder()
                        .section(table.getStringCellValue(row, SECTION))
                        .value(table.getCurrencyCellValue(row, VALUE))
                        .currency(table.getStringCellValue(row, CURRENCY))
                        .build());
    }

    enum CashTableHeader implements TableColumnDescription {
        SECTION("сектор"),
        VALUE("плановый исходящий остаток"),
        CURRENCY("валюта");

        @Getter
        private final TableColumn column;
        CashTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
