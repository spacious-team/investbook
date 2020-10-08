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

    BiPredicate<String, Object> CELL_STRING_STARTS_WITH = (cell, searchingValue) ->
            searchingValue != null && cell.trim().toLowerCase().startsWith(searchingValue.toString().trim().toLowerCase());

    /**
     * @return table table cell address or {@link TableCellAddress#NOT_FOUND}
     */
    default TableCellAddress find(Object value) {
        return find(value, 0);
    }

    /**
     * @return table table cell address or {@link TableCellAddress#NOT_FOUND}
     */
    default TableCellAddress find(Object value, int startRow) {
        return find(value, startRow, Integer.MAX_VALUE);
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this, can handle values greater than real rows count
     * @return table table cell address or {@link TableCellAddress#NOT_FOUND}
     */
    default TableCellAddress find(Object value, int startRow, int endRow) {
        return find(value, startRow, endRow, ReportPage.CELL_STRING_STARTS_WITH);
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this, can handle values greater than real rows count
     * @param stringPredicate cell and value comparing bi-predicate if cell value type is string
     * @return table table cell address or {@link TableCellAddress#NOT_FOUND}
     */
    default TableCellAddress find(Object value, int startRow, int endRow, BiPredicate<String, Object> stringPredicate) {
        return find(value, startRow, endRow, 0, Integer.MAX_VALUE, stringPredicate);
    }

    /**
     * @param value searching value
     * @param startRow search rows start from this
     * @param endRow search rows excluding this, can handle values greater than real rows count
     * @param startColumn search columns start from this
     * @param endColumn search columns excluding this, can handle values greater than real columns count
     * @return table table cell address or {@link TableCellAddress#NOT_FOUND}
     */
    TableCellAddress find(Object value, int startRow, int endRow,
                                         int startColumn, int endColumn,
                                         BiPredicate<String, Object> stringPredicate);


    /**
     * For vertical table of key-value records (table with two columns), search and return value for requested key.
     */
    default Object getNextColumnValue(String firstColumnValue) {
        TableCellAddress address = find(firstColumnValue);
        for (TableCell cell : getRow(address.getRow())) {
            if (cell != null && cell.getColumnIndex() > address.getColumn()) {
                Object value = cell.getValue();
                if (value != null && (!(value instanceof String) || !((String) value).isBlank())) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * @return row object or null is row does not exist
     */
    TableRow getRow(int i);

    int getLastRowNum();

    default TableCell getCell(TableCellAddress address) {
        return getRow(address.getRow()).getCell(address.getColumn());
    }

    /**
     * Get table range, table ends with predefined string in one of the row cells
     */
    default TableCellRange getTableCellRange(String tableName, int headersRowCount, String tableFooterString) {
        TableCellAddress startAddress = find(tableName);
        if (startAddress.equals(TableCellAddress.NOT_FOUND)) {
            return TableCellRange.EMPTY_RANGE;
        }
        TableCellAddress endAddress = find(tableFooterString, startAddress.getRow() + headersRowCount + 1,
                getLastRowNum(), ReportPage.CELL_STRING_STARTS_WITH);
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
                Object value;
                if (!(cell == null
                        || ((value = cell.getValue()) == null)
                        || (value instanceof String) && (value.toString().isEmpty()))) {
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
