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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.fountain.xelem.excel.Cell;
import nl.fountain.xelem.excel.Row;
import ru.investbook.parser.table.TableCell;
import ru.investbook.parser.table.TableRow;

import java.util.Iterator;

import static ru.investbook.parser.table.TableCellAddress.NOT_FOUND;

@RequiredArgsConstructor
public class XmlTableRow extends TableRow {

    @Getter
    private final Row row;

    @Override
    public TableCell getCell(int i) {
        Cell cell = row.getCellAt(i + 1);
        return (cell == null) ? null : new XmlTableCell(cell);
    }

    @Override
    public int getRowNum() {
        return row.getIndex() - 1;
    }

    @Override
    public int getFirstCellNum() {
        return row.getCellMap().firstKey() - 1;
    }

    @Override
    public int getLastCellNum() {
        return row.getCellMap().lastKey() - 1;
    }

    @Override
    public boolean rowContains(Object value) {
        return XmlTableHelper.find(row, value, 0, Integer.MAX_VALUE, String::equals) != NOT_FOUND;
    }

    @Override
    public Iterator<TableCell> iterator() {
        return new TableRowIterator<>(row.getCells().iterator(), XmlTableCell::new);
    }
}
