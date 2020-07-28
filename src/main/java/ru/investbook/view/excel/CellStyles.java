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

import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    public CellStyles(XSSFWorkbook book) {
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

    protected static XSSFCellStyle createHeaderStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        style.getFont().setBold(true);
        return style;
    }

    protected static XSSFCellStyle createLeftAlignedItalicTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createLeftAlignedTextStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    protected static XSSFCellStyle createLeftAlignedTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    protected static XSSFCellStyle createTotalRowStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createMoneyStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    protected static XSSFCellStyle createDateStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    protected static XSSFCellStyle createIntegerStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0_-;-* # ### ##0_-;_-* \"-\"??_-;_-@_-"));
        return style;
    }

    protected static XSSFCellStyle createMoneyStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0.00_р_._-;-* # ### ##0.00_р_._-;_-* \"-\"??_р_._-;_-@_-"));
        return style;
    }

    protected static XSSFCellStyle createPercentStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefaultStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("0.0%"));
        return style;
    }

    protected static XSSFCellStyle createDefaultStyle(XSSFWorkbook book) {
        Font font = book.createFont();
        XSSFCellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}
