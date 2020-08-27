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

package ru.investbook.parser.table.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellAddress;
import ru.investbook.parser.table.ReportPage;

import java.math.BigDecimal;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

class ExcelTableHelper {
    static final CellAddress NOT_FOUND = new CellAddress(-1, -1);

    static boolean rowContains(Row row, Object value) {
        return rowContains(row.getSheet(), row.getRowNum(), value);
    }

    private static boolean rowContains(Sheet sheet, int rowNum, Object value) {
        return find(sheet, value, rowNum, rowNum + 1, String::equals) != NOT_FOUND;
    }

    static CellAddress find(Sheet sheet, Object value) {
        return find(sheet, value, 0);
    }

    private static CellAddress find(Sheet sheet, Object value, int startRow) {
        return find(sheet, value, startRow, sheet.getLastRowNum());
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     */
    private static CellAddress find(Sheet sheet, Object value, int startRow, int endRow) {
        return find(sheet, value, startRow, endRow, ReportPage.CELL_STRING_EQUALS);
    }

    /**
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     * @param stringPredicate cell and value comparing bi-predicate if cell value type is string
     */
    static CellAddress find(Sheet sheet, Object value, int startRow, int endRow, BiPredicate<String, Object> stringPredicate) {
        return find(sheet, value, startRow, endRow, 0, Integer.MAX_VALUE, stringPredicate);
    }

    /**
     * @param value searching value
     * @param startRow search rows start from this
     * @param endRow search rows excluding this
     * @param startColumn search columns start from this
     * @param endColumn search columns excluding this
     */
    static CellAddress find(Sheet sheet, Object value, int startRow, int endRow,
                                   int startColumn, int endColumn,
                                   BiPredicate<String, Object> stringPredicate) {
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
                if (cell != null) {
                    int column = cell.getColumnIndex();
                    if (startColumn <= column && column < endColumn && cell.getCellType() == type) {
                        if (compare(value, cell, stringPredicate)) {
                            return cell.getAddress();
                        }
                    }
                }
            }
        }
        return NOT_FOUND;
    }

    private static CellAddress findByPredicate(Sheet sheet, int startRow, Predicate<Cell> predicate) {
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
        return switch (cell.getCellType()) {
            case BLANK -> value == null || value.equals("");
            case STRING -> stringPredicate.test(cell.getStringCellValue(), value);
            case NUMERIC -> value instanceof Number && cell.getNumericCellValue() == ((Number) value).doubleValue();
            case BOOLEAN -> value.equals(cell.getBooleanCellValue());
            default -> false;
        };
    }

    private static Cell getCell(Sheet sheet, CellAddress address) {
        return sheet.getRow(address.getRow()).getCell(address.getColumn());
    }

    static long getLongCellValue(Cell cell) {
        CellType type = cell.getCellType();
        return switch (type) {
            case NUMERIC -> Double.valueOf(cell.getNumericCellValue()).longValue();
            default -> Long.parseLong(cell.getStringCellValue());
        };
    }

    static BigDecimal getCurrencyCellValue(Cell cell) {
        double cellValue = cell.getNumericCellValue();
        return (Math.abs(cellValue - 0.01d) < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue);
    }

    static String getStringCellValue(Cell cell) {
        return (cell.getCellType() == CellType.BLANK) ? "" : cell.getStringCellValue();
    }
}
