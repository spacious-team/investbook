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

package ru.investbook.parser.tinkoff;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import static org.spacious_team.table_wrapper.api.TableCellAddress.NOT_FOUND;

class TinkoffBrokerReportHelper {
    private static final Pattern tableNamePattern = Pattern.compile("^[0-9]+\\.[0-9]+\\s+\\b");
    private static final Pattern pageNumberPattern = Pattern.compile("^[0-9]+\\s+из");

    /**
     * Удаляет строки содержащие номер страницы ("1 из 5") между названием таблицы и заголовком таблицы.
     * Необходимо для правильного парсинга таблиц.
     */
    static void removePageNumRows(ExcelSheet excelSheet) {
        List<Integer> rowsForCut = new ArrayList<>();
        Sheet sheet = excelSheet.getSheet();
        Iterator<Row> it = sheet.rowIterator();
        while (it.hasNext()) {
            Row row = it.next();
            int rowNum = row.getRowNum();
            // for perf checking page num rows only between Table name and Table header rows
            if (rowNum > 0 && isTableHeader(excelSheet, rowNum - 1)) {
                if(isPageRowNum(excelSheet, rowNum)) { // "1 из" - номер страницы, удаляем строку
                    rowsForCut.add(rowNum);
                }
            }
        }
        cutRows(sheet, rowsForCut);
    }

    private static boolean isTableHeader(ReportPage reportPage, int rowNum) {
        return reportPage.find(rowNum, rowNum + 1, 0, 2,  cell -> (cell instanceof String) &&
                !cell.toString().isEmpty() &&
                tableNamePattern.matcher(cell.toString()).lookingAt()) != NOT_FOUND;
    }

    private static boolean isPageRowNum(ReportPage excelSheet, int rowNum) {
        return excelSheet.find(rowNum, rowNum + 1, cell -> (cell instanceof String) &&
                isPageNumRow(cell.toString())) != NOT_FOUND;
    }

    private static boolean isPageNumRow(String value) {
        return StringUtils.hasLength(value) &&
                Character.isDigit(value.charAt(0)) &&
                pageNumberPattern.matcher(value).lookingAt();
    }

    private static void cutRows(Sheet sheet, List<Integer> rowsForCut) {
        removeRowMergedRegions(sheet, rowsForCut); // required for correct shift
        Collections.reverse(rowsForCut); // start from last row: do not change next cutting row num
        rowsForCut.forEach(rowNum -> cutRow(sheet, rowNum));
    }

    private static void removeRowMergedRegions(Sheet sheet, Collection<Integer> rowsForCut) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstRow = range.getFirstRow();
            if (firstRow == range.getLastRow() && rowsForCut.contains(firstRow)) {
                sheet.removeMergedRegion(i);
                i--;
            }
        }
    }

    private static void cutRow(Sheet sheet, int rowNum) {
        Row removingRow = sheet.getRow(rowNum);
        if (removingRow != null) {
            sheet.removeRow(removingRow);
        }
        int lastRowNum = sheet.getLastRowNum();
        if (rowNum >= 0 && rowNum < lastRowNum) {
            sheet.shiftRows(rowNum + 1, lastRowNum, -1);
        }
    }
}
