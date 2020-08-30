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

package ru.investbook.parser.table.xml;

import lombok.RequiredArgsConstructor;
import nl.fountain.xelem.excel.Row;
import nl.fountain.xelem.excel.Worksheet;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.TableCellAddress;
import ru.investbook.parser.table.TableRow;

import java.util.function.BiPredicate;

@RequiredArgsConstructor
public class XmlReportPage implements ReportPage {

    private final Worksheet sheet;

    @Override
    public TableCellAddress find(Object value, int startRow, int endRow, int startColumn, int endColumn,
                                 BiPredicate<String, Object> stringPredicate) {
        return XmlTableHelper.find(sheet, value, startRow, endRow, startColumn, endColumn, stringPredicate);
    }

    @Override
    public TableRow getRow(int i) {
        Row row = sheet.getRowAt(i);
        return (row == null) ? null : new XmlTableRow(row);
    }

    @Override
    public int getLastRowNum() {
        return XmlTableHelper.getLastRowNum(sheet);
    }
}
