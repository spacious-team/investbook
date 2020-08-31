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

import nl.fountain.xelem.excel.Cell;
import ru.investbook.parser.TableColumnDescription;
import ru.investbook.parser.table.AbstractTable;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.TableCellRange;
import ru.investbook.parser.table.TableRow;

import java.math.BigDecimal;

public class XmlTable extends AbstractTable {

    XmlTable(ReportPage reportPage,
                       String tableName,
                       TableCellRange tableRange,
                       Class<? extends TableColumnDescription> headerDescription,
                       int headersRowCount) {
        super(reportPage, tableName, tableRange, headerDescription, headersRowCount);
    }

    @Override
    public Object getCellValue(TableRow row, TableColumnDescription columnDescription) {
        return getRawCell((XmlTableRow) row, columnDescription).getData();
    }

    @Override
    public int getIntCellValue(TableRow row, TableColumnDescription columnDescription) {
        Double numberValue = (Double) getCellValue(row, columnDescription);
        return numberValue.intValue();
    }

    @Override
    public long getLongCellValue(TableRow row, TableColumnDescription columnDescription) {
        Double numberValue = (Double) getCellValue(row, columnDescription);
        return numberValue.longValue();
    }

    @Override
    public BigDecimal getCurrencyCellValue(TableRow row, TableColumnDescription columnDescription) {
        double cellValue = (double) getCellValue(row, columnDescription);
        return (Math.abs(cellValue - 0.01d) < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue);
    }

    @Override
    public String getStringCellValue(TableRow row, TableColumnDescription columnDescription) {
        return getRawCell((XmlTableRow) row, columnDescription).getData$();
    }

    private Cell getRawCell(XmlTableRow row, TableColumnDescription columnDescription) {
        return row.getRow().getCellAt(columnIndices.get(columnDescription.getColumn()) + 1);
    }
}
