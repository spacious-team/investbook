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

package ru.investbook.view.excel;

import ru.investbook.view.TableHeader;

public interface ExcelTableHeader extends TableHeader {
    String ROW_NUM_PLACE_HOLDER = "{rowNum}";

    default String getCellAddr() {
        return getColumnIndex() + ROW_NUM_PLACE_HOLDER;
    }

    default char getColumnIndex() {
        return  (char) ('A' + this.ordinal());
    }

    default String getRange(int startRowNum, int endRowNum) {
        return "" + getColumnIndex() + startRowNum + ":" + getColumnIndex() + endRowNum;
    }
}
