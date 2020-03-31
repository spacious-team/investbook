package ru.portfolio.portfolio.view.excel;

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

    public CellStyles(XSSFWorkbook book) {
        this.defaultStyle = createDefalutStyle(book);
        this.headerStyle = createHeaderStyle(book);
        this.totalTextStyle = createLeftAlignedItalicTextStyle(book);
        this.totalRowStyle = createTotalRowStyle(book);
        this.leftAlignedTextStyle = createLeftAlignedTextStyle(book);
        this.dateStyle = createDateStyle(book);
        this.moneyStyle = createMoneyStyle(book);
        this.intStyle = createIntegerStyle(book);
    }

    protected static XSSFCellStyle createHeaderStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.getFont().setBold(true);
        return style;
    }

    protected static XSSFCellStyle createLeftAlignedItalicTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createLeftAlignedTextStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    protected static XSSFCellStyle createLeftAlignedTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    protected static XSSFCellStyle createTotalRowStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createMoneyStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    protected static XSSFCellStyle createDateStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    protected static XSSFCellStyle createIntegerStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0_-;-* # ### ##0_-;_-* \"-\"??_-;_-@_-"));
        return style;
    }

    protected static XSSFCellStyle createMoneyStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0.00_р_._-;-* # ### ##0.00_р_._-;_-* \"-\"??_р_._-;_-@_-"));
        return style;
    }

    protected static XSSFCellStyle createDefalutStyle(XSSFWorkbook book) {
        Font font = book.createFont();
        XSSFCellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}
