/*
 * Portfolio
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

package ru.portfolio.portfolio.parser;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class ExcelTableHelper {
    public static final CellRangeAddress EMTPY_RANGE = new CellRangeAddress(-1, -1, -1, -1);
    public static final CellAddress NOT_FOUND = new CellAddress(-1, -1);

    public static CellRangeAddress getTableCellRange(Sheet sheet, String tableName, String tableFooterString) {
        CellAddress startAddress = find(sheet, tableName);
        if (startAddress.equals(NOT_FOUND)) {
            return EMTPY_RANGE;
        }
        CellAddress endAddress = find(sheet, tableFooterString, startAddress.getRow() + 2,
                sheet.getLastRowNum(), (cell , prefix) -> cell.startsWith(prefix.toString()));
        if (endAddress.equals(NOT_FOUND)) {
            return EMTPY_RANGE;
        }
        return new CellRangeAddress(
                startAddress.getRow(),
                endAddress.getRow(),
                sheet.getRow(startAddress.getRow()).getFirstCellNum(),
                sheet.getRow(endAddress.getRow()).getLastCellNum());
    }

    /**
     * Get table ragne, table ends with empty line
     */
    public static CellRangeAddress getTableCellRange(Sheet sheet, String tableName) {
        CellAddress startAddress = find(sheet, tableName);
        if (startAddress.equals(NOT_FOUND)) {
            return EMTPY_RANGE;
        }
        int lastRowNum = startAddress.getRow() + 1;
        LAST_ROW:
        for(; lastRowNum < sheet.getLastRowNum(); lastRowNum++) {
            Row row = sheet.getRow(lastRowNum);
            if (row == null || row.getLastCellNum() == 0) {
                break;
            }
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() != CellType.BLANK) {
                    continue LAST_ROW;
                }
            }
            break; // is all row cells blank
        }
        lastRowNum--; // exclude last row from table
        if (lastRowNum < startAddress.getRow()) lastRowNum = startAddress.getRow();
        return new CellRangeAddress(
                startAddress.getRow(),
                lastRowNum,
                sheet.getRow(startAddress.getRow()).getFirstCellNum(),
                sheet.getRow(lastRowNum).getLastCellNum());
    }

    public static boolean rowContains(ExcelTable table, Row row, Object value) {
        return rowContains(table.getSheet(), row.getRowNum(), value);
    }

    public static boolean rowContains(Sheet sheet, int rowNum, Object value) {
        return find(sheet, value, rowNum, rowNum + 1, String::equals) != NOT_FOUND;
    }

    public static CellAddress find(Sheet sheet, Object value) {
        return find(sheet, value, 0);
    }

    public static CellAddress find(Sheet sheet, Object value, int startRow) {
        return find(sheet, value, startRow, sheet.getLastRowNum());
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     */
    public static CellAddress find(Sheet sheet, Object value, int startRow, int endRow) {
        return find(sheet, value, startRow, endRow, String::equals);
    }

    /**
     * @param stringPredicate cell and value comparing bi-predicate if cell value type is string
     */
    public static CellAddress find(Sheet sheet, Object value, int startRow, int endRow, BiPredicate<String, Object> stringPredicate) {
        if (sheet.getLastRowNum() == -1) {
            return NOT_FOUND;
        } else if (endRow > sheet.getLastRowNum()) {
            endRow = sheet.getLastRowNum();
        }
        CellType type = getType(value);
        if (type == CellType.NUMERIC) {
            value = ((Number) value).doubleValue();
        }
        for(int rowNum = startRow; rowNum < endRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() == type) {
                    if (compare(value, cell, stringPredicate)) {
                        return cell.getAddress();
                    }
                }
            }
        }
        return NOT_FOUND;
    }

    public static CellAddress findByPredicate(Sheet sheet, int startRow, Predicate<Cell> predicate) {
        int endRow = sheet.getLastRowNum();
        for(int rowNum = startRow; rowNum < endRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null) continue;
            for (Cell cell : row) {
                if (predicate.test(cell)) {
                    return cell.getAddress();
                }
            }
        }
        return NOT_FOUND;
    }

    private static CellType getType(Object value) {
        CellType type;
        if (value instanceof String) {
            type = (((String) value).isEmpty()) ? CellType.BLANK : CellType.STRING;
        } else if (value instanceof Number) {
            type = CellType.NUMERIC;
        } else if (value instanceof Boolean) {
            type = CellType.BOOLEAN;
        } else if (value == null) {
            type = CellType.BLANK;
        } else {
            throw new IllegalArgumentException("Не могу сравнить значение '" + value + "' типа " + value.getClass().getName());
        }
        return type;
    }

    private static boolean compare(Object value, Cell cell, BiPredicate<String, Object> stringPredicate) {
        switch (cell.getCellType()) {
            case BLANK:
                if (value == null || value.equals("")) return true;
                return false;
            case STRING:
                if (stringPredicate.test(cell.getStringCellValue(), value)) return true;
                return false;
            case NUMERIC:
                if (value instanceof Number && cell.getNumericCellValue() == ((Number) value).doubleValue()) return true;
                return false;
            case BOOLEAN:
                if (value.equals(cell.getBooleanCellValue())) return true;
                return false;
        }
        return false;
    }

    public static Cell getCell(Sheet sheet, CellAddress address) {
        return sheet.getRow(address.getRow()).getCell(address.getColumn());
    }

}
