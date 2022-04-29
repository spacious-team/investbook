/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableHeader;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static ru.investbook.report.excel.ExcelConditionalFormatHelper.highlightNegativeByRed;
import static ru.investbook.report.excel.ExcelFormulaHelper.sumAbsValues;
import static ru.investbook.report.excel.StockMarketProfitExcelTableHeader.*;

@Component
public class StockMarketProfitExcelTableView extends ExcelTableView {

    @Getter
    private final boolean summaryView = false;
    @Getter
    private final int sheetOrder = 5;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> portfolio + " (фондовый)";
    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public StockMarketProfitExcelTableView(PortfolioRepository portfolioRepository,
                                           StockMarketProfitExcelTableFactory tableFactory,
                                           PortfolioConverter portfolioConverter,
                                           TransactionCashFlowRepository transactionCashFlowRepository) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.transactionCashFlowRepository = transactionCashFlowRepository;
    }

    @Override
    protected Collection<ExcelTable> createExcelTables(Portfolio portfolio, String sheetName) {
        List<String> currencies = getCurrencies(portfolio);
        Collection<ExcelTable> tables = new ArrayList<>(currencies.size());
        for (String currency : currencies) {
            Table table = tableFactory.create(portfolio, currency);
            String sheetNameWithCurrency = sheetName + " " + currency;
            tables.add(ExcelTable.of(portfolio, sheetNameWithCurrency, table, this));
        }
        return tables;
    }

    private List<String> getCurrencies(Portfolio portfolio) {
        return transactionCashFlowRepository
                .findDistinctCurrencyByPortfolioAndCashFlowType(portfolio.getId(), CashFlowType.PRICE);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(SECURITY.ordinal(), 45 * 256);
        sheet.setColumnWidth(OPEN_AMOUNT.ordinal(), 16 * 256);
        sheet.setColumnWidth(CLOSE_AMOUNT.ordinal(), 16 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record totalRow = new Table.Record();
        for (StockMarketProfitExcelTableHeader column : StockMarketProfitExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(SECURITY, "Итого:");
        totalRow.put(COUNT, sumAbsValues(COUNT, 3, table.size() + 2));
        totalRow.remove(OPEN_DATE);
        totalRow.remove(CLOSE_DATE);
        totalRow.remove(OPEN_PRICE);
        totalRow.remove(YIELD);
        return totalRow;
    }

    @Override
    protected void sheetPreCreate(Sheet sheet, Table table) {
        super.sheetPreCreate(sheet, table);
        if (table.stream().noneMatch(record -> record.containsKey(TAX_LIABILITY))) {
            // Брокеры являются агентами по акциям отечественных бумаг на мосбирже
            sheet.setColumnHidden(TAX_LIABILITY.ordinal(), true); // нет обязательств
        } else {
            sheet.setZoom(89); // show all columns for 24 inch monitor for securities sheet
        }
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
        highlightNegativeByRed(sheet, YIELD);
    }
}
