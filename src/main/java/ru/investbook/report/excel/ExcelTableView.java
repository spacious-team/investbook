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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.beans.factory.annotation.Value;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.TableHeader;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.PortfolioRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ru.investbook.report.excel.StockMarketProfitExcelTableHeader.ROW_NUM_PLACE_HOLDER;

@Slf4j
@RequiredArgsConstructor
public abstract class ExcelTableView {
    protected final PortfolioRepository portfolioRepository;
    protected final TableFactory tableFactory;
    protected final PortfolioConverter portfolioConverter;
    private final Pattern camelCaseWordBoundaryPattern = Pattern.compile("(?<=[a-z])(?=[A-Z][a-z])");
    private final Pattern invalidExcelSheetNameChars = Pattern.compile("[^0-9a-zA-Zа-яА-Я\\s()]");
    @Value("${server.port}")
    private int serverPort;

    public Collection<ExcelTable> createExcelTables() {
        Collection<ExcelTable> tables = new ArrayList<>();
        for (PortfolioEntity entity : getPortfolios(ViewFilter.get().getPortfolios())) {
            Portfolio portfolio = portfolioConverter.fromEntity(entity);
            String sheetName = getSheetNameCreator().apply(portfolio.getId());
            tables.addAll(createExcelTables(portfolio, sheetName));
        }
        return tables;
    }

    protected Collection<ExcelTable> createExcelTables(Portfolio portfolio, String sheetName) {
        Table table = tableFactory.create(portfolio);
        return Collections.singleton(ExcelTable.of(portfolio, sheetName, table, this));
    }

    protected Collection<PortfolioEntity> getPortfolios(Collection<String> allowedPortfolios) {
        // TODO select by user
        Collection<PortfolioEntity> portfolios = portfolioRepository.findAll();
        if (CollectionUtils.isNotEmpty(allowedPortfolios)) {
            return portfolios.stream()
                    .filter(e -> allowedPortfolios.contains(e.getId()))
                    .collect(Collectors.toSet());
        }
        return portfolios;
    }

    protected abstract UnaryOperator<String> getSheetNameCreator();

    public abstract int getSheetOrder();

    /**
     * Thread safe method
     * @param portfolio accept null or portfolio
     */
    public void createSheet(Portfolio portfolio,
                                     Workbook book,
                                     String sheetName,
                                     Table table,
                                     CellStyles styles) {
        if (table == null || table.isEmpty()) return;
        Class<? extends TableHeader> headerType = getHeaderType(table);
        if (headerType == null) return;
        synchronized (book) {
            long t0 = System.nanoTime();
            Sheet sheet = book.createSheet(validateExcelSheetName(sheetName));
            writeHeader(sheet, headerType, styles.getHeaderStyle());
            sheetPreCreate(sheet, table);
            Table.Record totalRow = getTotalRow(table, Optional.ofNullable(portfolio));
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
                        } else if (value instanceof LocalDate) {
                            cell.setCellValue((LocalDate) value);
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
            log.debug("Вкладка '{}' сохранена за {}", sheetName, Duration.ofNanos(System.nanoTime() - t0));
        }
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

    private String validateExcelSheetName(String name) {
        return invalidExcelSheetNameChars.matcher(name).replaceAll("-");
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

    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        return new Table.Record();
    }

    protected void sheetPreCreate(Sheet sheet, Table table) {
        sheet.setZoom(93); // show all columns for 24 inch monitor for securities sheet
    }

    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, (headerType.getEnumConstants().length - 1)));
    }
}
