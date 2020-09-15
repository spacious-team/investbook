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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.Portfolio;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import java.util.List;
import java.util.function.UnaryOperator;

import static ru.investbook.view.excel.StockMarketProfitExcelTableHeader.*;

@Component
public class StockMarketProfitExcelTableView extends ExcelTableView {

    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public StockMarketProfitExcelTableView(PortfolioRepository portfolioRepository,
                                           StockMarketProfitExcelTableFactory tableFactory,
                                           PortfolioConverter portfolioConverter,
                                           TransactionCashFlowRepository transactionCashFlowRepository) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.transactionCashFlowRepository = transactionCashFlowRepository;
    }

    @Override
    protected void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator, Portfolio portfolio) {
        List<String> currencies = transactionCashFlowRepository
                .findDistinctCurrencyByPkPortfolioAndPkType(portfolio.getId(), CashFlowType.PRICE);
        for (String currency : currencies) {
            Table table = tableFactory.create(portfolio, currency);
            if (!table.isEmpty()) {
                Sheet sheet = book.createSheet(validateExcelSheetName(sheetNameCreator.apply(portfolio.getId()) + " " + currency));
                writeTable(table, sheet, styles);
            }
        }
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(SECURITY.ordinal(), 45 * 256);
        sheet.setColumnWidth(OPEN_AMOUNT.ordinal(), 16 * 256);
        sheet.setColumnWidth(CLOSE_AMOUNT.ordinal(), 16 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table) {
        Table.Record totalRow = new Table.Record();
        for (StockMarketProfitExcelTableHeader column : StockMarketProfitExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(SECURITY, "Итого:");
        totalRow.put(COUNT, "=SUMPRODUCT(ABS(" + COUNT.getRange(3, table.size() + 2) + "))");
        totalRow.remove(OPEN_DATE);
        totalRow.remove(CLOSE_DATE);
        totalRow.remove(OPEN_PRICE);
        totalRow.remove(YIELD);
        return totalRow;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(SECURITY.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == SECURITY.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
