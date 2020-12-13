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

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import java.util.function.UnaryOperator;

import static ru.investbook.view.excel.ExcelTableHeader.getColumnsRange;
import static ru.investbook.view.excel.PortfolioAnalysisExcelTableHeader.*;

@Component
@Slf4j
public class PortfolioAnalysisExcelTableView extends ExcelTableView {

    public PortfolioAnalysisExcelTableView(PortfolioRepository portfolioRepository,
                                           PortfolioAnalysisExcelTableFactory tableFactory,
                                           PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    public void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator) {
        Table table = ((PortfolioAnalysisExcelTableFactory) tableFactory).create();
        if (!table.isEmpty()) {
            Sheet sheet = book.createSheet(validateExcelSheetName("Обзор"));
            writeTable(table, sheet, styles);
        }
        super.writeTo(book, styles, sheetNameCreator);
    }

    @Override
    protected void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator, Portfolio portfolio) {
        Table table = tableFactory.create(portfolio);
        if (!table.isEmpty()) {
            Sheet sheet = book.createSheet(validateExcelSheetName(sheetNameCreator.apply(portfolio.getId())));
            writeTable(table, sheet, styles);
        }
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(INVESTMENT_CURRENCY.ordinal(), 17 * 256);
        sheet.setColumnWidth(ASSETS_RUB.ordinal(), 17 * 256);
        sheet.setColumnWidth(ASSETS_USD.ordinal(), 17 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table) {
        Table.Record totalRow = Table.newRecord();
        totalRow.put(DATE, "Итого:");
        totalRow.put(TOTAL_INVESTMENT_USD,  getLastValue(table, TOTAL_INVESTMENT_USD));
        totalRow.put(CASH_RUB, getLastCashValue(table, CASH_RUB));
        totalRow.put(CASH_USD, getLastCashValue(table, CASH_USD));
        totalRow.put(CASH_EUR, getLastCashValue(table, CASH_EUR));
        totalRow.put(CASH_GBP, getLastCashValue(table, CASH_GBP));
        totalRow.put(CASH_CHF, getLastCashValue(table, CASH_CHF));
        totalRow.put(TOTAL_CASH_USD, getLastCashValue(table, TOTAL_CASH_USD));
        totalRow.put(ASSETS_RUB, getLastValue(table, ASSETS_RUB));
        totalRow.put(ASSETS_USD, getLastValue(table, ASSETS_USD));
        totalRow.put(ASSETS_GROWTH, getLastValue(table, ASSETS_GROWTH));
        totalRow.put(SP500_GROWTH, getLastValue(table, SP500_GROWTH));
        return totalRow;
    }

    private String getLastCashValue(Table table, ExcelTableHeader column) {
        return "=INDEX(" +
                getColumnsRange(CASH_RUB, 3, TOTAL_CASH_USD, table.size() + 2) + "," +
                "MATCH(1E+99," + TOTAL_CASH_USD.getRange(3, table.size() + 2) + ")," +
                (Math.abs(column.ordinal() - CASH_RUB.ordinal()) + 1) + ")";
    }

    private static String getLastValue(Table table, ExcelTableHeader column) {
        return "=LOOKUP(2,1/(" + column.getRange(3, table.size() + 2) + "<>0)," + column.getRange(3, table.size() + 2) + ")";
    }

    @Override
    protected void sheetPreCreate(Sheet sheet, Table table) {
        super.sheetPreCreate(sheet, table);
        sheet.setZoom(102); // show all columns for 24 inch monitor for securities sheet
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Cell cell : sheet.getRow(1)) {
                cell.setCellStyle(styles.getTotalRowStyle());
        }
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell;
            if ((cell = row.getCell(ASSETS_GROWTH.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
            if ((cell = row.getCell(SP500_GROWTH.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
        }
    }
}
