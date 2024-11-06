/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableHeader;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static ru.investbook.report.excel.ExcelChartPlotHelper.*;
import static ru.investbook.report.excel.ExcelConditionalFormatHelper.highlightNegativeByRed;
import static ru.investbook.report.excel.ExcelFormulaHelper.sumAbsValues;
import static ru.investbook.report.excel.PortfolioStatusExcelTableHeader.*;

@Component
@Slf4j
public class PortfolioStatusExcelTableView extends ExcelTableView {

    @Getter
    private final boolean summaryView = true;
    @Getter
    private final int sheetOrder = 1;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> "Портфель (" + portfolio + ")";

    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final Set<Integer> types = Set.of(CashFlowType.PRICE.getId(), CashFlowType.DERIVATIVE_PRICE.getId());

    public PortfolioStatusExcelTableView(PortfolioRepository portfolioRepository,
                                         PortfolioStatusExcelTableFactory tableFactory,
                                         PortfolioConverter portfolioConverter,
                                         TransactionCashFlowRepository transactionCashFlowRepository) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.transactionCashFlowRepository = transactionCashFlowRepository;
    }

    @Override
    public Collection<ExcelTable> createExcelTables() {
        Collection<ExcelTable> tables = new ArrayList<>(createExcelTablesByCurrencies());
        if (!showOnlySummary(ViewFilter.get())) {
            tables.addAll(super.createExcelTables());
        }
        return tables;
    }

    private Collection<ExcelTable> createExcelTablesByCurrencies() {
        ViewFilter filter = ViewFilter.get();
        Collection<String> portfolios = filter.getPortfolios();
        if (showOnlySummary(filter) || isManyPortfolioRequested(portfolios)) {
            Collection<ExcelTable> tables = new ArrayList<>();
            List<String> currencies = portfolios.isEmpty() ?
                    transactionCashFlowRepository.findDistinctCurrencyByCashFlowTypeIn(types) :
                    transactionCashFlowRepository.findDistinctCurrencyByPortfolioInAndCashFlowTypeIn(portfolios, types);
            for (String currency : currencies) {
                Table table = tableFactory.create(portfolios, currency);
                String sheetName = "Портфель (все) " + currency;
                tables.add(ExcelTable.of(sheetName, table, this));
            }
            return tables;
        }
        return emptyList();
    }

    private boolean isManyPortfolioRequested(Collection<String> portfolios) {
        return portfolios.size() > 1 || (portfolios.isEmpty() && portfolioRepository.count() > 1);
    }

    private static boolean showOnlySummary(ViewFilter filter) {
        return !filter.isShowDetails();
    }

    @Override
    protected Collection<ExcelTable> createExcelTables(Portfolio portfolio, String sheetName) {
        List<String> currencies = transactionCashFlowRepository.findDistinctCurrencyByPortfolioInAndCashFlowTypeIn(
                singleton(portfolio.getId()), types);
        return currencies.stream()
                .map(currency -> createExcelTables(portfolio, sheetName, currency))
                .collect(toList());
    }

    private ExcelTable createExcelTables(Portfolio portfolio, String sheetName, String currency) {
        Table table = tableFactory.create(portfolio, currency);
        String sheetNameWithCurrency = sheetName + " " + currency;
        return ExcelTable.of(portfolio, sheetNameWithCurrency, table, this);
    }

    @Override
    protected <T extends Enum<T> & TableHeader> void writeHeader(Sheet sheet, Class<T> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        TableHeader[] tableHeader = requireNonNull(headerType.getEnumConstants());
        for (TableHeader header : tableHeader) {
            sheet.setColumnWidth(header.ordinal(), 15 * 256);
        }
        sheet.setColumnWidth(SECURITY.ordinal(), 44 * 256);
        sheet.setColumnWidth(TYPE.ordinal(), 19 * 256);
        sheet.setColumnHidden(TYPE.ordinal(), true);
        sheet.setColumnWidth(FIRST_TRANSACTION_DATE.ordinal(), 12 * 256);
        sheet.setColumnWidth(LAST_TRANSACTION_DATE.ordinal(), 12 * 256);
        sheet.setColumnWidth(BUY_COUNT.ordinal(), 12 * 256);
        sheet.setColumnWidth(CELL_COUNT.ordinal(), 12 * 256);
        sheet.setColumnWidth(COUNT.ordinal(), 14 * 256);
        sheet.setColumnWidth(AVERAGE_PRICE.ordinal(), 15 * 256);
        sheet.setColumnWidth(AVERAGE_ACCRUED_INTEREST.ordinal(), 14 * 256);
        sheet.setColumnWidth(COMMISSION.ordinal(), 14 * 256);
        sheet.setColumnWidth(LAST_EVENT_DATE.ordinal(), 14 * 256);
        sheet.setColumnWidth(AMORTIZATION.ordinal(), 16 * 256);
        sheet.setColumnWidth(LAST_ACCRUED_INTEREST.ordinal(), 13 * 256);
        sheet.setColumnWidth(TAX.ordinal(), 19 * 256);
        sheet.setColumnWidth(INTERNAL_RATE_OF_RETURN.ordinal(), (int) (12.5 * 256));
        sheet.setColumnWidth(PROFIT_PROPORTION.ordinal(), 11 * 256);
        sheet.setColumnWidth(INVESTMENT_PROPORTION.ordinal(), 11 * 256);
        sheet.setColumnWidth(PROPORTION.ordinal(), 11 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record totalRow = Table.newRecord();
        for (PortfolioStatusExcelTableHeader column : PortfolioStatusExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(SECURITY, "Итого:");
        totalRow.put(COUNT, sumAbsValues(COUNT, 3, table.size() + 2 - 1 /* without cash row */));
        totalRow.remove(FIRST_TRANSACTION_DATE);
        totalRow.remove(LAST_TRANSACTION_DATE);
        totalRow.remove(LAST_EVENT_DATE);
        totalRow.remove(AVERAGE_PRICE);
        totalRow.remove(AVERAGE_ACCRUED_INTEREST);
        totalRow.remove(LAST_PRICE);
        totalRow.remove(LAST_ACCRUED_INTEREST);
        totalRow.remove(INTERNAL_RATE_OF_RETURN);
        return totalRow;
    }

    @Override
    protected void sheetPreCreate(Sheet sheet, Table table) {
        super.sheetPreCreate(sheet, table);
        sheet.setZoom(81); // show all columns for 24-inch monitor for securities sheet
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell;
            if ((cell = row.getCell(SECURITY.ordinal())) != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
            if ((cell = row.getCell(INTERNAL_RATE_OF_RETURN.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
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
            } else if (cell.getColumnIndex() == BUY_COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == CELL_COUNT.ordinal()) {
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
        highlightNegativeByRed(sheet, PROFIT);
        highlightNegativeByRed(sheet, INTERNAL_RATE_OF_RETURN);
        plotChart("Состав портфеля", sheet, PortfolioStatusExcelTableView::addPieChart);
    }

    private static void addPieChart(String name, XSSFSheet sheet) {
        int rowCount = sheet.getLastRowNum();

        XDDFDataSource<String> securities = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                nonEmptyCellRangeAddress(sheet,2, rowCount, SECURITY.ordinal(), SECURITY.ordinal()));
        XDDFNumericalDataSource<Double> proportions = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                nonEmptyCellRangeAddress(sheet,2, rowCount, PROPORTION.ordinal(), PROPORTION.ordinal()));

        XSSFChart chart = createChart(sheet, name, 0, rowCount + 2, PROPORTION.ordinal() + 1, 36);
        XDDFChartData data = createPieChartData(chart);

        data.addSeries(securities, proportions);
        chart.plot(data);
    }
}
