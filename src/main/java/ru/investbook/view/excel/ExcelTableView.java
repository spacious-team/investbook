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

package ru.investbook.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.beans.factory.annotation.Value;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;
import ru.investbook.view.TableHeader;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static ru.investbook.view.excel.StockMarketProfitExcelTableHeader.ROW_NUM_PLACE_HOLDER;

@RequiredArgsConstructor
public abstract class ExcelTableView {
    protected final PortfolioRepository portfolioRepository;
    protected final TableFactory tableFactory;
    protected final PortfolioConverter portfolioConverter;
    private final Pattern camelCaseWordBoundaryPattern = Pattern.compile("(?<=[a-z])(?=[A-Z][a-z])");
    private final Pattern invalidExcelSheetNameChars = Pattern.compile("[^0-9a-zA-Zа-яА-Я\\s()]");
    @Getter
    @Setter
    private Portfolio portfolio;
    @Value("${server.port}")
    private int serverPort;

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
            Sheet sheet = book.createSheet(validateExcelSheetName(sheetNameCreator.apply(portfolio.getId())));
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

    protected String validateExcelSheetName(String name) {
        return invalidExcelSheetNameChars.matcher(name).replaceAll("-");
    }

    protected void writeTable(Table table,
                              Sheet sheet,
                              CellStyles styles) {
        if (table == null || table.isEmpty()) return;
        Class<? extends TableHeader> headerType = getHeaderType(table);
        if (headerType == null) return;
        writeHeader(sheet, headerType, styles.getHeaderStyle());
        sheetPreCreate(sheet, table);
        Table.Record totalRow = getTotalRow(table);
        if (totalRow != null && !totalRow.isEmpty()) {
            table.addFirst(totalRow);
        }
        int rowNum = 0;
        for (Map<? extends TableHeader, Object> tableRow : table) {
            Row row = sheet.createRow(++rowNum);
            for (TableHeader header : headerType.getEnumConstants()) {
                Object value = tableRow.get(header);
                if (value == null) {
                    continue;
                }
                Cell cell = row.createCell(header.ordinal());
                try {
                    if (value instanceof String) {
                        String string = (String) value;
                        if (string.startsWith("=")) {
                            string = string.substring(1)
                                    .replace(ROW_NUM_PLACE_HOLDER, String.valueOf(rowNum + 1));
                            value = string;
                            cell.setCellFormula(string);
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
                } catch (Exception e) {
                    throw new RuntimeException("Не могу задать значение '" + value + "'" +
                            " в ячейку " + cell.getAddress() +
                            " на вкладке '" + sheet.getSheetName() + "'");
                }
            }
        }
        sheetPostCreate(sheet, headerType, styles);
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
        CreationHelper createHelper = sheet.getWorkbook().getCreationHelper();
        for (TableHeader header : headerType.getEnumConstants()) {
            Cell cell = row.createCell(header.ordinal());
            cell.setCellValue(header.getDescription());
            cell.setCellStyle(style);
            sheet.setColumnWidth(header.ordinal(), 14 * 256);
            Hyperlink link = getHyperlink(createHelper, header);
            cell.setHyperlink(link);
        }
        sheet.createFreezePane(0, 1);
    }

    protected Hyperlink getHyperlink(CreationHelper createHelper, TableHeader header) {
        Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
        String pageNameInCamelCase = this.getClass()
                .getSimpleName()
                .replace("ExcelTableView", "");
        String pageName = camelCaseWordBoundaryPattern.matcher(pageNameInCamelCase)
                .replaceAll("-")
                .toLowerCase();
        String fragmentId = header.toString().toLowerCase().replace("_", "-");
        link.setAddress("http://localhost" + ((serverPort == 80) ? "" : (":" + serverPort)) +
                "/user-guide/" + pageName + ".html#" + fragmentId);
        return link;
    }

    protected Table.Record getTotalRow(Table table) {
        return new Table.Record();
    }

    protected void sheetPreCreate(Sheet sheet, Table table) {
        sheet.setZoom(93); // show all columns for 24 inch monitor for securities sheet
    }

    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, (headerType.getEnumConstants().length - 1)));
    }
}
