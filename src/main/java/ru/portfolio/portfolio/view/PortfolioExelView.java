package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.repository.PortfolioRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import static ru.portfolio.portfolio.view.ExcelProfitSheetHeader.ROW_NUM_PLACE_HOLDER;

@Component
@RequiredArgsConstructor
public class PortfolioExelView {
    private final PortfolioRepository portfolioRepository;
    private final TransactionProfitTableFactory transactionProfitTableFactory;

    public void writeTo(Path path) throws IOException {
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFCellStyle defaultStyle = createDefalutStyle(book);
        XSSFCellStyle headerStyle = createHeaderStyle(book);
        XSSFCellStyle totalTextStyle = createLeftAlignedItalicTextStyle(book);
        XSSFCellStyle totalRowStyle = createTotalRowStyle(book);
        XSSFCellStyle securityNameStyle = createLeftAlignedTextStyle(book);
        XSSFCellStyle dateStyle = createDateStyle(book);
        XSSFCellStyle moneyStyle = createMoneyStyle(book);
        XSSFCellStyle intStyle = createIntegerStyle(book);
        for (PortfolioEntity portfolio : portfolioRepository.findAll()) {
            Deque<Map<ExcelProfitSheetHeader, Object>> profitTable = transactionProfitTableFactory.calculatePortfolioProfit(portfolio);
            XSSFSheet sheet = book.createSheet(portfolio.getPortfolio());
            writeHeader(sheet, headerStyle);
            profitTable.addFirst(getTotalRow());
            int rowNum = 0;
            for (Map<ExcelProfitSheetHeader, Object> transactionProfit : profitTable) {
                XSSFRow row = sheet.createRow(++rowNum);
                for (ExcelProfitSheetHeader header : ExcelProfitSheetHeader.values()) {
                    Object value = transactionProfit.get(header);
                    if (value == null) {
                        continue;
                    }
                    XSSFCell cell = row.createCell(header.ordinal());
                    if (value instanceof String) {
                        String string = (String) value;
                        if (string.startsWith("=")) {
                            cell.setCellFormula(string.substring(1)
                                    .replace(ROW_NUM_PLACE_HOLDER, String.valueOf(rowNum + 1)));
                            cell.setCellType(CellType.FORMULA);
                            cell.setCellStyle(moneyStyle);
                        } else {
                            cell.setCellValue(string);
                            cell.setCellStyle(defaultStyle);
                        }
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                        cell.setCellStyle(moneyStyle);
                    } else if (value instanceof Instant) {
                        cell.setCellValue(((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime());
                        cell.setCellStyle(dateStyle);
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                        cell.setCellStyle(defaultStyle);
                    }
                    if (rowNum == 1) {
                        if (header == ExcelProfitSheetHeader.SECURITY) {
                            cell.setCellStyle(totalTextStyle);
                        } else {
                            cell.setCellStyle(totalRowStyle);
                        }
                    } else if (header == ExcelProfitSheetHeader.SECURITY) {
                        cell.setCellStyle(securityNameStyle);
                    } else if (header == ExcelProfitSheetHeader.COUNT) {
                        cell.setCellStyle(intStyle);
                    }
                }
            }
        }
        book.write(Files.newOutputStream(path));
        book.close();
    }

    private void writeHeader(XSSFSheet sheet, XSSFCellStyle style) {
        XSSFRow row = sheet.createRow(0);
        row.setHeight((short)-1);
        for (ExcelProfitSheetHeader header : ExcelProfitSheetHeader.values()) {
            XSSFCell cell = row.createCell(header.ordinal());
            cell.setCellValue(header.getDescription());
            cell.setCellStyle(style);
            sheet.setColumnWidth(header.ordinal(), 14 * 256);
        }
        sheet.setColumnWidth(0, 45 * 256);
        sheet.setColumnWidth(4, 16 * 256);
        sheet.setColumnWidth(8, 16 * 256);
    }

    private Map<ExcelProfitSheetHeader, Object> getTotalRow() {
        Map<ExcelProfitSheetHeader, Object> totalRow = new HashMap<>();
        for (ExcelProfitSheetHeader column : ExcelProfitSheetHeader.values()) {
            totalRow.put(column, "=SUM(" +
                    column.getColumnIndex() + "3:" +
                    column.getColumnIndex() + "100000)");
        }
        totalRow.put(ExcelProfitSheetHeader.SECURITY, "Итого:");
        totalRow.remove(ExcelProfitSheetHeader.BUY_DATE);
        totalRow.remove(ExcelProfitSheetHeader.CELL_DATE);
        totalRow.remove(ExcelProfitSheetHeader.BUY_PRICE);
        totalRow.remove(ExcelProfitSheetHeader.PROFIT);
        return totalRow;
    }

    private static XSSFCellStyle createHeaderStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.getFont().setBold(true);
        return style;
    }

    private static XSSFCellStyle createLeftAlignedItalicTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createLeftAlignedTextStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    private static XSSFCellStyle createLeftAlignedTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private static XSSFCellStyle createTotalRowStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createMoneyStyle(book);
        style.getFont().setItalic(true);
        return style;
    }

    private static XSSFCellStyle createDateStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("dd.mm.yyyy"));
        return style;
    }

    private static XSSFCellStyle createIntegerStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0_-;-* # ### ##0_-;_-* \"-\"??_-;_-@_-"));
        return style;
    }

    private static XSSFCellStyle createMoneyStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        CreationHelper createHelper = book.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("_-* # ### ##0.00_р_._-;-* # ### ##0.00_р_._-;_-* \"-\"??_р_._-;_-@_-"));
        return style;
    }

    private static XSSFCellStyle createDefalutStyle(XSSFWorkbook book) {
        XSSFFont font = book.createFont();
        XSSFCellStyle style = book.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }
}
