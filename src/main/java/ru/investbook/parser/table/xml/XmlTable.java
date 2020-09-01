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
import java.util.regex.Pattern;

public class XmlTable extends AbstractTable {

    private static final Pattern spacePattern = Pattern.compile("\\s");

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
        return (int) getLongCellValue(row, columnDescription);
    }

    @Override
    public long getLongCellValue(TableRow row, TableColumnDescription columnDescription) {
        Object cellValue = getCellValue(row, columnDescription);
        if (cellValue instanceof Number) {
            return ((Number) cellValue).longValue();
        } else {
            return Long.parseLong(spacePattern.matcher(cellValue.toString()).replaceAll(""));
        }
    }

    @Override
    public BigDecimal getCurrencyCellValue(TableRow row, TableColumnDescription columnDescription) {
        Object cellValue = getCellValue(row, columnDescription);
        double number;
        if (cellValue instanceof Number) {
            number = ((Number) cellValue).doubleValue();
        } else {
            number = Double.parseDouble(spacePattern.matcher(cellValue.toString()).replaceAll(""));
        }
        return (Math.abs(number - 0.01d) < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(number);
    }

    @Override
    public String getStringCellValue(TableRow row, TableColumnDescription columnDescription) {
        return getRawCell((XmlTableRow) row, columnDescription).getData$();
    }

    private Cell getRawCell(XmlTableRow row, TableColumnDescription columnDescription) {
        return row.getRow().getCellAt(columnIndices.get(columnDescription.getColumn()) + 1);
    }
}
