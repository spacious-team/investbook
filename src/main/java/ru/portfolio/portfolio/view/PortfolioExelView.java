package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static ru.portfolio.portfolio.view.TransactionProfitTableFactory.*;

@Component
@RequiredArgsConstructor
public class PortfolioExelView {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityRepository securityRepository;
    private static final String[] HEADER = new String[]{
            SECURITY, BUY_DATE, COUNT, BUY_PRICE, BUY_AMOUNT,
            BUY_ACCRUED_INTEREST, BUY_COMMISSION, CELL_DATE, CELL_AMOUNT, CELL_ACCRUED_INTEREST,
            ACCRUED_INTEREST, AMORTIZATION, DIVIDEND,
            CELL_COMMISSION, TAX, PROFIT};
    private final TransactionProfitTableFactory transactionProfitTableFactory;

    public void writeTo(Path path) throws IOException {
        XSSFWorkbook book = new XSSFWorkbook();
        XSSFCellStyle defaultStyle = createDefalutStyle(book);
        XSSFCellStyle headerStyle = createHeaderStyle(book);
        XSSFCellStyle securityNameStyle = createLeftAlignedTextStyle(book);
        XSSFCellStyle dateStyle = createDateStyle(book);
        XSSFCellStyle moneyStyle = createMoneyStyle(book);
        XSSFCellStyle intStyle = createIntegerStyle(book);
        for (PortfolioEntity portfolio : portfolioRepository.findAll()) {
            List<Map<String, Object>> profitTable = transactionProfitTableFactory.calculatePortfolioProfit(portfolio);
            XSSFSheet sheet = book.createSheet(portfolio.getPortfolio());
            writeHeader(sheet, headerStyle);
            int rowNum = 1;
            for (Map<String, Object> transactionProfit : profitTable) {
                XSSFRow row = sheet.createRow(rowNum++);
                for (int i = 0; i < HEADER.length; i++) {
                    Object value = transactionProfit.get(HEADER[i]);
                    if (value == null) {
                        continue;
                    }
                    XSSFCell cell = row.createCell(i);
                    if (value instanceof String) {
                        cell.setCellValue((String) value);
                        cell.setCellStyle(defaultStyle);
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
                    if (i == 0) {
                        cell.setCellStyle(securityNameStyle);
                    } else if (i == 2) {
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
        for (int i = 0; i < HEADER.length; i++) {
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(HEADER[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 14 * 256);
        }
        sheet.setColumnWidth(0, 45 * 256);
    }

    private static XSSFCellStyle createHeaderStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.getFont().setBold(true);
        return style;
    }

    private static XSSFCellStyle createLeftAlignedTextStyle(XSSFWorkbook book) {
        XSSFCellStyle style = createDefalutStyle(book);
        style.setAlignment(HorizontalAlignment.LEFT);
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
