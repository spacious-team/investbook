package ru.portfolio.portfolio.view.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.view.ProfitTable;
import ru.portfolio.portfolio.view.ProfitTableHeader;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static ru.portfolio.portfolio.view.excel.StockMarketExcelProfitTableHeader.ROW_NUM_PLACE_HOLDER;

public abstract class ExcelProfitTable {

    public void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator) {
        for (PortfolioEntity portfolio : getPortfolios()) {
            ProfitTable profitTable = getProfitTable(portfolio);
            if (!profitTable.isEmpty()) {
                Sheet sheet = book.createSheet(sheetNameCreator.apply(portfolio.getPortfolio()));
                writeProfitTable(profitTable, sheet, styles);
            }
        }
    }

    protected abstract List<PortfolioEntity> getPortfolios();

    protected abstract ProfitTable getProfitTable(PortfolioEntity portfolio);

    protected void writeProfitTable(ProfitTable profitTable,
                                    Sheet sheet,
                                    CellStyles styles) {
        if (profitTable.isEmpty()) return;
        Class<? extends ProfitTableHeader> headerType = getHeaderType(profitTable);
        writeHeader(sheet, headerType, styles.getHeaderStyle());
        ProfitTable.Record totalRow = getTotalRow();
        if (totalRow != null && !totalRow.isEmpty()) {
            profitTable.addFirst(totalRow);
        }
        int rowNum = 0;
        for (Map<? extends ProfitTableHeader, Object> transactionProfit : profitTable) {
            Row row = sheet.createRow(++rowNum);
            for (ProfitTableHeader header : headerType.getEnumConstants()) {
                Object value = transactionProfit.get(header);
                if (value == null) {
                    continue;
                }
                Cell cell = row.createCell(header.ordinal());
                if (value instanceof String) {
                    String string = (String) value;
                    if (string.startsWith("=")) {
                        cell.setCellFormula(string.substring(1)
                                .replace(ROW_NUM_PLACE_HOLDER, String.valueOf(rowNum + 1)));
                        cell.setCellType(CellType.FORMULA);
                        cell.setCellStyle(styles.getMoneyStyle());
                    } else {
                        cell.setCellValue(string);
                        cell.setCellStyle(styles.getDefaultStyle());
                    }
                } else if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                    if (value instanceof Integer || value instanceof Long
                            || value instanceof Short || value instanceof Byte) {
                        cell.setCellStyle(styles.getIntStyle());
                    } else {
                        cell.setCellStyle(styles.getMoneyStyle());
                    }
                } else if (value instanceof Instant) {
                    cell.setCellValue(((Instant) value).atZone(ZoneId.systemDefault()).toLocalDateTime());
                    cell.setCellStyle(styles.getDateStyle());
                } else if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                    cell.setCellStyle(styles.getDefaultStyle());
                }
            }
        }
        sheetPostCreate(sheet, styles);
    }

    private Class<? extends ProfitTableHeader> getHeaderType(ProfitTable profitTable) {
        if (profitTable.isEmpty()) return null;
        return profitTable.peek()
                .keySet()
                .iterator()
                .next()
                .getClass();
    }

    protected void writeHeader(Sheet sheet, Class<? extends ProfitTableHeader> headerType, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeight((short)-1);
        for (ProfitTableHeader header : headerType.getEnumConstants()) {
            Cell cell = row.createCell(header.ordinal());
            cell.setCellValue(header.getDescription());
            cell.setCellStyle(style);
            sheet.setColumnWidth(header.ordinal(), 14 * 256);
        }
    }

    protected ProfitTable.Record getTotalRow() {
        return new ProfitTable.Record();
    }

    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
    }
}
