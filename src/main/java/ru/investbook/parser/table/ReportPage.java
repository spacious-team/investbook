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

package ru.investbook.parser.table;

import java.util.function.BiPredicate;

public interface ReportPage {

    TableCellAddress find(Object value);

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     * @param stringPredicate cell and value comparing bi-predicate if cell value type is string
     */
    TableCellAddress find(Object value, int startRow, int endRow, BiPredicate<String, Object> stringPredicate);

    TableRow getRow(int Row);

    int getLastRowNum();

    String getStringCellValue(TableCell cell);

    long getLongCellValue(TableCell cell);

    BiPredicate<String, Object> CELL_STRING_EQUALS = (cell, searchingValue) ->
            searchingValue != null && cell.trim().toLowerCase().startsWith(searchingValue.toString().trim().toLowerCase());

    default TableCellRange getTableCellRange(String tableName, int headersRowCount, String tableFooterString) {
        TableCellAddress startAddress = find(tableName);
        if (startAddress.equals(TableCellAddress.NOT_FOUND)) {
            return TableCellRange.EMPTY_RANGE;
        }
        TableCellAddress endAddress = find(tableFooterString, startAddress.getRow() + headersRowCount + 1,
                getLastRowNum(), ReportPage.CELL_STRING_EQUALS);
        if (endAddress.equals(TableCellAddress.NOT_FOUND)) {
            return TableCellRange.EMPTY_RANGE;
        }
        return new TableCellRange(
                startAddress.getRow(),
                endAddress.getRow(),
                getRow(startAddress.getRow()).getFirstCellNum(),
                getRow(endAddress.getRow()).getLastCellNum());
    }

    /**
     * Get table range, table ends with empty line
     */
    default TableCellRange getTableCellRange(String tableName, int headersRowCount) {
        TableCellAddress startAddress = find(tableName);
        if (startAddress.equals(TableCellAddress.NOT_FOUND)) {
            return TableCellRange.EMPTY_RANGE;
        }
        int lastRowNum = startAddress.getRow() + headersRowCount + 1;
        LAST_ROW:
        for(int n = getLastRowNum(); lastRowNum < n; lastRowNum++) {
            TableRow row = getRow(lastRowNum);
            if (row == null || row.getLastCellNum() == 0) {
                break; // all row cells blank
            }
            for (TableCell cell : row) {
                if (!(cell == null
                        || cell.getCellType() == TableCellType.BLANK
                        || (cell.getCellType() == TableCellType.STRING && cell.getStringCellValue().isEmpty()))) {
                    // not empty
                    continue LAST_ROW;
                }
            }
            break; // all row cells blank
        }
        lastRowNum--; // exclude last row from table
        if (lastRowNum < startAddress.getRow()) lastRowNum = startAddress.getRow();
        return new TableCellRange(
                startAddress.getRow(),
                lastRowNum,
                getRow(startAddress.getRow()).getFirstCellNum(),
                getRow(lastRowNum).getLastCellNum());
    }
}
