/*
 * Portfolio
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

package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.portfolio.portfolio.converter.PortfolioConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;
import ru.portfolio.portfolio.view.TableHeader;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static ru.portfolio.portfolio.view.excel.StockMarketProfitExcelTableHeader.ROW_NUM_PLACE_HOLDER;

@RequiredArgsConstructor
public abstract class ExcelTableView {
    protected final PortfolioRepository portfolioRepository;
    protected final TableFactory tableFactory;
    protected final PortfolioConverter portfolioConverter;
    @Getter
    @Setter
    private Portfolio portfolio;

    public void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator) {
        for (PortfolioEntity entity : getPortfolios()) {
            Portfolio portfolio = portfolioConverter.fromEntity(entity);
            setPortfolio(portfolio);
            writeTo(book, styles, sheetNameCreator, portfolio);
        }
    }

    protected void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator, Portfolio portfolio) {
        Table table = getTable(portfolio);
        if (!table.isEmpty()) {
            Sheet sheet = book.createSheet(sheetNameCreator.apply(portfolio.getId()));
            writeTable(table, sheet, styles);
        }
    }

    protected List<PortfolioEntity> getPortfolios() {
        // TODO select by user
        return portfolioRepository.findAll();
    }

    protected Table getTable(Portfolio portfolio) {
        return tableFactory.create(portfolio);
    }

    protected void writeTable(Table table,
                              Sheet sheet,
                              CellStyles styles) {
        if (table.isEmpty()) return;
        Class<? extends TableHeader> headerType = getHeaderType(table);
        writeHeader(sheet, headerType, styles.getHeaderStyle());
        Table.Record totalRow = getTotalRow();
        if (totalRow != null && !totalRow.isEmpty()) {
            table.addFirst(totalRow);
        }
        int rowNum = 0;
        for (Map<? extends TableHeader, Object> transactionProfit : table) {
            Row row = sheet.createRow(++rowNum);
            for (TableHeader header : headerType.getEnumConstants()) {
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

    private Class<? extends TableHeader> getHeaderType(Table table) {
        for (Table.Record record : table) {
            if (record.isEmpty()) continue;
            return record.keySet()
                    .iterator()
                    .next()
                    .getClass();
        }
        return null;
    }

    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        Row row = sheet.createRow(0);
        row.setHeight((short)-1);
        for (TableHeader header : headerType.getEnumConstants()) {
            Cell cell = row.createCell(header.ordinal());
            cell.setCellValue(header.getDescription());
            cell.setCellStyle(style);
            sheet.setColumnWidth(header.ordinal(), 14 * 256);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, (headerType.getEnumConstants().length - 1)));
    }

    protected Table.Record getTotalRow() {
        return new Table.Record();
    }

    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
        sheet.setZoom(93); // show all columns for 24 inch monitor for securities sheet
    }
}
