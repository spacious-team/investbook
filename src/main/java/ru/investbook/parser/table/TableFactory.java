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

import ru.investbook.parser.TableColumnDescription;

public interface TableFactory {

    boolean canHandle(ReportPage reportPage);

    default Table create(ReportPage reportPage, String tableName, String tableFooterString,
                         Class<? extends TableColumnDescription> headerDescription) {
        return create(reportPage, tableName, tableFooterString, headerDescription, 1);
    }

    default Table create(ReportPage reportPage, String tableName,
                         Class<? extends TableColumnDescription> headerDescription) {
        return create(reportPage, tableName, headerDescription, 1);
    }

    Table create(ReportPage reportPage, String tableName, String tableFooterString,
                 Class<? extends TableColumnDescription> headerDescription,
                 int headersRowCount);

    Table create(ReportPage reportPage, String tableName,
                 Class<? extends TableColumnDescription> headerDescription,
                 int headersRowCount);

    Table createOfNoName(ReportPage reportPage, String madeUpTableName, String firstLineText,
                         Class<? extends TableColumnDescription> headerDescription,
                         int headersRowCount);

    default TableCellRange getNoNameTableRange(ReportPage reportPage, String firstLineText, int headersRowCount) {
        TableCellRange range = reportPage.getTableCellRange(firstLineText, headersRowCount);
        if (!range.equals(TableCellRange.EMPTY_RANGE)) {
            range = new TableCellRange(range.getFirstRow() - 1, range.getLastRow(),
                    range.getFirstColumn(), range.getLastColumn());
        }
        return range;
    }
}
