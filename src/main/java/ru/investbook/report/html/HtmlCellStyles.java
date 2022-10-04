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

package ru.investbook.report.html;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import ru.investbook.report.excel.CellStyles;

class HtmlCellStyles extends CellStyles {

    HtmlCellStyles(Workbook book) {
        super(book);
    }

    protected CellStyle createIntegerStyle(Workbook book) {
        CellStyle style = super.createIntegerStyle(book);
        style.setDataFormat(book.getCreationHelper()
                .createDataFormat()
                .getFormat("_-* # ### ##0_-;-* # ### ##0_-;_-* # ### ##0_-;_-@_-"));
        return style;
    }

    protected CellStyle createMoneyStyle(Workbook book) {
        CellStyle style = super.createMoneyStyle(book);
        style.setDataFormat(book.getCreationHelper()
                .createDataFormat()
                .getFormat("_-* # ### ##0.00_р_._-;-* # ### ##0.00_р_._-;_-* # ### ##0.00_р_._-;_-@_-"));
        return style;
    }
}
