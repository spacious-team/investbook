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

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import ru.investbook.parser.TableColumnDescription;
import ru.investbook.parser.table.*;

import java.math.BigDecimal;

@Slf4j
@ToString(callSuper = true)
public class ExcelTable extends AbstractTable {

    public static ExcelTable of(ReportPage reportPage, String tableName, String tableFooterString,
                                Class<? extends TableColumnDescription> headerDescription) {
        return of(reportPage, tableName, tableFooterString, headerDescription, 1);
    }

    public static ExcelTable of(ReportPage reportPage, String tableName,
                                Class<? extends TableColumnDescription> headerDescription) {
        return of(reportPage, tableName, headerDescription, 1);
    }

    public static ExcelTable of(ReportPage reportPage, String tableName, String tableFooterString,
                                Class<? extends TableColumnDescription> headerDescription,
                                int headersRowCount) {
        ExcelTable table = new ExcelTable(reportPage, tableName,
                reportPage.getTableCellRange(tableName, headersRowCount, tableFooterString),
                headerDescription,
                headersRowCount);
        table.setLastTableRowContainsTotalData(true);
        return table;
    }

    public static ExcelTable of(ReportPage reportPage, String tableName,
                                Class<? extends TableColumnDescription> headerDescription,
                                int headersRowCount) {
        ExcelTable table = new ExcelTable(reportPage, tableName,
                reportPage.getTableCellRange(tableName, headersRowCount),
                headerDescription,
                headersRowCount);
        table.setLastTableRowContainsTotalData(false);
        return table;
    }

    public static ExcelTable ofNoName(ReportPage reportPage, String madeUpTableName, String firstLineText,
                                      Class<? extends TableColumnDescription> headerDescription,
                                      int headersRowCount) {
        TableCellRange range = reportPage.getTableCellRange(firstLineText, headersRowCount);
        if (!range.equals(TableCellRange.EMPTY_RANGE)) {
            range = new TableCellRange(range.getFirstRow() - 1, range.getLastRow(),
                    range.getFirstColumn(), range.getLastColumn());
        }
        ExcelTable table = new ExcelTable(reportPage, madeUpTableName, range, headerDescription, headersRowCount);
        table.setLastTableRowContainsTotalData(true);
        return table;
    }

    private ExcelTable(ReportPage reportPage, String tableName, TableCellRange tableRange,
                       Class<? extends TableColumnDescription> headerDescription, int headersRowCount) {
        super(reportPage, tableName, tableRange, headerDescription, headersRowCount);
    }

    public TableCell getCell(TableRow row, TableColumnDescription columnDescription) {
        return row.getCell(columnIndices.get(columnDescription.getColumn()));
    }

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    public int getIntCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, int defaultValue) {
        return (int) getLongCellValueOrDefault(row, columnDescription, defaultValue);
    }

    public int getIntCellValue(TableRow row, TableColumnDescription columnDescription) {
        return (int) getLongCellValue(row, columnDescription);
    }

    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    public long getLongCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, long defaultValue) {
        try {
            return getLongCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public long getLongCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getLongCellValue(getRawCell(row, columnDescription));
    }
    /**
     * @return return cell value or defaultValue if the cell is missing or the type does not match the expected
     */
    public BigDecimal getCurrencyCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, BigDecimal defaultValue) {
        try {
            return getCurrencyCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public BigDecimal getCurrencyCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getCurrencyCellValue(getRawCell(row, columnDescription));
    }

    public String getStringCellValueOrDefault(TableRow row, TableColumnDescription columnDescription, String defaultValue) {
        try {
            return getStringCellValue(row, columnDescription);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getStringCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getStringCellValue(getRawCell(row, columnDescription));
    }

    private Cell getRawCell(TableRow row, TableColumnDescription columnDescription) {
        return ((ExcelTableRow) row).getRow().getCell(columnIndices.get(columnDescription.getColumn()));
    }
}
