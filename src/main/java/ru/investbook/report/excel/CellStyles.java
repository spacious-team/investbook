/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

import lombok.Getter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

@Getter
public class CellStyles {
    private final CellStyle defaultStyle;
    private final CellStyle headerStyle;
    private final CellStyle totalTextStyle;
    private final CellStyle totalRowStyle;
    private final CellStyle leftAlignedTextStyle;
    private final CellStyle dateStyle;
    private final CellStyle moneyStyle;
    private final CellStyle intStyle;
    private final CellStyle percentStyle;

    public CellStyles(Workbook book) {
        this.defaultStyle = createDefaultStyle(book);
        this.headerStyle = createHeaderStyle(book);
        this.totalTextStyle = createLeftAlignedItalicTextStyle(book);
        this.totalRowStyle = createTotalRowStyle(book);
        this.leftAlignedTextStyle = createLeftAlignedTextStyle(book);
        this.dateStyle = createDateStyle(book);
        this.moneyStyle = createMoneyStyle(book);
        this.intStyle = createIntegerStyle(book);
        this.percentStyle = createPercentStyle(book);
    }

    protected static CellStyle createHeaderStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        Font font = book.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    protected static CellStyle createLeftAlignedItalicTextStyle(Workbook book) {
        CellStyle style = createLeftAlignedTextStyle(book);
        Font font = book.createFont();
        font.setItalic(true);
        style.setFont(font);
        return style;
    }

    protected static CellStyle createLeftAlignedTextStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    protected static CellStyle createTotalRowStyle(Workbook book) {
        CellStyle style = createMoneyStyle(book);
        Font font = book.createFont();
        font.setItalic(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    protected static CellStyle createDateStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    protected static CellStyle createIntegerStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setIndention((short) 1);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0_-;-* # ### ##0_-;_-* \"-\"??_-;_-@_-"));
        return style;
    }

    protected static CellStyle createMoneyStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setIndention((short) 1);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0.00_р_._-;-* # ### ##0.00_р_._-;_-* \"-\"??_р_._-;_-@_-"));
        return style;
    }

    protected static CellStyle createPercentStyle(Workbook book) {
        CellStyle style = createDefaultStyle(book);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setIndention((short) 1);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("0.0%"));
        return style;
    }

    protected static CellStyle createDefaultStyle(Workbook book) {
        Font font = book.createFont();
        CellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}
