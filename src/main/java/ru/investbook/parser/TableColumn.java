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

package ru.investbook.parser;

import org.apache.poi.ss.usermodel.Row;

public interface TableColumn {
    TableColumn NOCOLUMN = (i, j) -> -1;

    default TableColumn ofOptional(TableColumn column) {
        return AnyOfTableColumn.of(column, TableColumn.NOCOLUMN);
    }

    /**
     * @param headerRows header rows
     * @return column index of table
     */
    default int getColumnIndex(Row... headerRows) {
        return getColumnIndex(0, headerRows);
    }

    /**
     * @param firstColumnForSearch start result column search from this index
     * @param headerRows header rows
     * @return column index of table
     */
    int getColumnIndex(int firstColumnForSearch, Row... headerRows);
}
