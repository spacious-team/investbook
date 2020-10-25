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
import org.apache.poi.ss.usermodel.Cell;
import ru.investbook.parser.TableColumnDescription;
import ru.investbook.parser.table.AbstractTable;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.TableCellRange;
import ru.investbook.parser.table.TableRow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@ToString(callSuper = true)
public class ExcelTable extends AbstractTable {

    ExcelTable(ReportPage reportPage,
               String tableName,
               TableCellRange tableRange,
               Class<? extends TableColumnDescription> headerDescription,
               int headersRowCount) {
        super(reportPage, tableName, tableRange, headerDescription, headersRowCount);
    }

    @Override
    public Object getCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getCellValue(getRawCell(row, columnDescription));
    }

    @Override
    public int getIntCellValue(TableRow row, TableColumnDescription columnDescription) {
        return (int) getLongCellValue(row, columnDescription);
    }

    @Override
    public long getLongCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getLongCellValue(getRawCell(row, columnDescription));
    }

    @Override
    public BigDecimal getCurrencyCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getCurrencyCellValue(getRawCell(row, columnDescription));
    }

    @Override
    public String getStringCellValue(TableRow row, TableColumnDescription columnDescription) {
        return ExcelTableHelper.getStringCellValue(getRawCell(row, columnDescription));
    }

    public Date getDateCellValue(TableRow row, TableColumnDescription columnDescription) {
        return getRawCell(row, columnDescription).getDateCellValue();
    }

    public LocalDateTime getLocalDateTimeCellValue(TableRow row, TableColumnDescription columnDescription) {
        return getRawCell(row, columnDescription).getLocalDateTimeCellValue();
    }

    private Cell getRawCell(TableRow row, TableColumnDescription columnDescription) {
        return ((ExcelTableRow) row).getRow().getCell(columnIndices.get(columnDescription.getColumn()));
    }
}
