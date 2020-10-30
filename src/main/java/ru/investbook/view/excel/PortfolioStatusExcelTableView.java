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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import static ru.investbook.view.excel.PortfolioStatusExcelTableHeader.*;

@Component
@Slf4j
public class PortfolioStatusExcelTableView extends ExcelTableView {

    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public PortfolioStatusExcelTableView(PortfolioRepository portfolioRepository,
                                         PortfolioStatusExcelTableFactory tableFactory,
                                         PortfolioConverter portfolioConverter,
                                         TransactionCashFlowRepository transactionCashFlowRepository) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.transactionCashFlowRepository = transactionCashFlowRepository;
    }

    @Override
    protected void writeTo(XSSFWorkbook book, CellStyles styles, UnaryOperator<String> sheetNameCreator, Portfolio portfolio) {
        List<String> currencies = transactionCashFlowRepository.findDistinctCurrencyByPkPortfolioAndPkTypeIn(
                portfolio.getId(),
                Set.of(CashFlowType.PRICE.getId(), CashFlowType.DERIVATIVE_PRICE.getId()));
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
        for (TableHeader header : headerType.getEnumConstants()) {
            sheet.setColumnWidth(header.ordinal(), 15 * 256);
        }
        sheet.setColumnWidth(SECURITY.ordinal(), 45 * 256);
        sheet.setColumnWidth(TYPE.ordinal(), 19 * 256);
        sheet.setColumnHidden(TYPE.ordinal(), true);
        sheet.setColumnWidth(FIRST_TRANSACTION_DATE.ordinal(), 12 * 256);
        sheet.setColumnWidth(LAST_TRANSACTION_DATE.ordinal(), 12 * 256);
        sheet.setColumnWidth(BUY_COUNT.ordinal(), 12 * 256);
        sheet.setColumnWidth(CELL_COUNT.ordinal(), 12 * 256);
        sheet.setColumnWidth(COUNT.ordinal(), 14 * 256);
        sheet.setColumnWidth(AVERAGE_PRICE.ordinal(), 14 * 256);
        sheet.setColumnWidth(AVERAGE_ACCRUED_INTEREST.ordinal(), 14 * 256);
        sheet.setColumnWidth(COMMISSION.ordinal(), 12 * 256);
        sheet.setColumnWidth(LAST_EVENT_DATE.ordinal(), 14 * 256);
        sheet.setColumnWidth(AMORTIZATION.ordinal(), 16 * 256);
        sheet.setColumnWidth(LAST_PRICE.ordinal(), 13 * 256);
        sheet.setColumnWidth(LAST_ACCRUED_INTEREST.ordinal(), 13 * 256);
        sheet.setColumnWidth(TAX.ordinal(), 19 * 256);
        sheet.setColumnWidth(PROFIT_PROPORTION.ordinal(), 11 * 256);
        sheet.setColumnWidth(INVESTMENT_PROPORTION.ordinal(), 11 * 256);
        sheet.setColumnWidth(PROPORTION.ordinal(), 11 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table) {
        Table.Record totalRow = new Table.Record();
        for (PortfolioStatusExcelTableHeader column : PortfolioStatusExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(SECURITY, "Итого:");
        totalRow.put(COUNT, "=SUMPRODUCT(ABS(" + COUNT.getRange(3, table.size() + 2 - 1 /* without cash row */) + "))");
        totalRow.remove(FIRST_TRANSACTION_DATE);
        totalRow.remove(LAST_TRANSACTION_DATE);
        totalRow.remove(LAST_EVENT_DATE);
        totalRow.remove(AVERAGE_PRICE);
        totalRow.remove(AVERAGE_ACCRUED_INTEREST);
        totalRow.remove(LAST_PRICE);
        totalRow.remove(LAST_ACCRUED_INTEREST);
        return totalRow;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        sheet.setZoom(85); // show all columns for 24 inch monitor for securities sheet
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell;
            if ((cell = row.getCell(SECURITY.ordinal())) != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
            if ((cell = row.getCell(PROFIT_PROPORTION.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
            if ((cell = row.getCell(INVESTMENT_PROPORTION.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
            if ((cell = row.getCell(PROPORTION.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == SECURITY.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == BUY_COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == CELL_COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == PROFIT_PROPORTION.ordinal()) {
                cell.setCellStyle(styles.getPercentStyle());
            } else if (cell.getColumnIndex() == INVESTMENT_PROPORTION.ordinal()) {
                cell.setCellStyle(styles.getPercentStyle());
            } else if (cell.getColumnIndex() == PROPORTION.ordinal()) {
                cell.setCellStyle(styles.getPercentStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
        addPieChart(sheet);
    }

    private void addPieChart(Sheet _sheet) {
        try {
            int chartHeight = 36;
            XSSFSheet sheet = (XSSFSheet) _sheet;
            int rowCount = sheet.getLastRowNum();
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0,
                    0, rowCount + 2, PROPORTION.ordinal() + 1, rowCount + 2 + chartHeight);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Состав портфеля");
            chart.setTitleOverlay(false);
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            XDDFDataSource<String> securities = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                    new CellRangeAddress(2, rowCount, SECURITY.ordinal(), SECURITY.ordinal()));
            XDDFNumericalDataSource<Double> proportions = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(2, rowCount, PROPORTION.ordinal(), PROPORTION.ordinal()));

            XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
            data.setVaryColors(true);
            data.addSeries(securities, proportions);
            chart.plot(data);
        } catch (Exception e) {
            String message = "Не могу построить график на вкладке '{}', возможно закрыты все позиции";
            log.info(message, _sheet.getSheetName());
            log.debug(message, _sheet.getSheetName(), e);
        }
    }
}
