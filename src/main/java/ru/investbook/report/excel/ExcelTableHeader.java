/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.report.excel;

import ru.investbook.report.TableHeader;

public interface ExcelTableHeader extends TableHeader {
    String ROW_NUM_PLACE_HOLDER = "{rowNum}";

    default String getCellAddr() {
        return getColumnIndex() + ROW_NUM_PLACE_HOLDER;
    }

    default String getCellAddr(int rowNum) {
        return "" + getColumnIndex() + rowNum;
    }

    default char getColumnIndex() {
        return  (char) ('A' + this.ordinal());
    }

    default String getColumnRange() {
        return "" + getColumnIndex() + ":" + getColumnIndex();
    }

    default String getRange(int startRowNum, int endRowNum) {
        return "" + getColumnIndex() + startRowNum + ":" + getColumnIndex() + endRowNum;
    }

    static String getColumnsRange(ExcelTableHeader startColumn, int startRow, ExcelTableHeader endColumn, int endRow) {
        return "" + startColumn.getColumnIndex() + startRow + ":" + endColumn.getColumnIndex() + endRow;
    }
}
