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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
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
import static ru.investbook.report.ForeignExchangeRateService.RUB;
import static ru.investbook.report.excel.DerivativesMarketTotalProfitExcelTableHeader.*;
import static ru.investbook.report.excel.ExcelChartPlotHelper.*;
import static ru.investbook.report.excel.ExcelConditionalFormatHelper.highlightNegativeByRed;

@Component
@Slf4j
public class DerivativesMarketTotalProfitExcelTableView extends ExcelTableView {

    @Getter
    private final boolean summaryView = true;
    @Getter
    private final int sheetOrder = 2;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> "Срочный обзорно (" + portfolio + ")";

    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final Set<Integer> types = Set.of(CashFlowType.DERIVATIVE_PRICE.getId());

    public DerivativesMarketTotalProfitExcelTableView(PortfolioRepository portfolioRepository,
                                                      DerivativesMarketTotalProfitExcelTableFactory tableFactory,
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
                    transactionCashFlowRepository.findDistinctCurrencyByPkTypeIn(types) :
                    transactionCashFlowRepository.findDistinctCurrencyByPkPortfolioAndPkTypeIn(portfolios, types);
            if (!currencies.contains(RUB)) currencies.add(RUB);
            for (String currency : currencies) {
                Table table = tableFactory.create(portfolios, currency);
                String sheetName = "Срочный обзорно " + currency;
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
        List<String> currencies = transactionCashFlowRepository.findDistinctCurrencyByPkPortfolioAndPkTypeIn(
                singleton(portfolio.getId()), types);
        if (!currencies.contains(RUB)) currencies.add(RUB);
        Collection<ExcelTable> tables = new ArrayList<>(currencies.size());
        for (String currency : currencies) {
            Table table = tableFactory.create(portfolio, currency);
            String sheetNameWithCurrency = sheetName + " " + currency;
            tables.add(ExcelTable.of(portfolio, sheetNameWithCurrency, table, this));
        }
        return tables;
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        for (TableHeader header : headerType.getEnumConstants()) {
            sheet.setColumnWidth(header.ordinal(), 16 * 256);
        }
        sheet.setColumnWidth(CONTRACT_GROUP.ordinal(), 24 * 256);
        sheet.setColumnWidth(GROSS_PROFIT_PNT.ordinal(), 19 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record totalRow = Table.newRecord();
        for (DerivativesMarketTotalProfitExcelTableHeader column : DerivativesMarketTotalProfitExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(CONTRACT_GROUP, "Итого:");
        totalRow.put(COUNT, "=SUMPRODUCT(ABS(" + COUNT.getRange(3, table.size() + 2) + "))");
        totalRow.remove(FIRST_TRANSACTION_DATE);
        totalRow.remove(LAST_TRANSACTION_DATE);
        totalRow.remove(LAST_EVENT_DATE);
        totalRow.remove(GROSS_PROFIT_PNT);
        return totalRow;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell;
            if ((cell = row.getCell(CONTRACT_GROUP.ordinal())) != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
            if ((cell = row.getCell(PROFIT_PROPORTION.ordinal())) != null) {
                cell.setCellStyle(styles.getPercentStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == CONTRACT_GROUP.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == BUY_COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == CELL_COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else if (cell.getColumnIndex() == PROFIT_PROPORTION.ordinal()) {
                cell.setCellStyle(styles.getPercentStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
        highlightNegativeByRed(sheet, PROFIT);
        plotChart("Прибыль", sheet, DerivativesMarketTotalProfitExcelTableView::addPieChart);
    }

    private static void addPieChart(String name, XSSFSheet sheet) {
        int rowCount = sheet.getLastRowNum();

        XDDFDataSource<String> securities = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                nonEmptyCellRangeAddress(sheet,2, rowCount, CONTRACT_GROUP.ordinal(), CONTRACT_GROUP.ordinal()));
        XDDFNumericalDataSource<Double> proportions = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                nonEmptyCellRangeAddress(sheet,2, rowCount, PROFIT_PROPORTION.ordinal(), PROFIT_PROPORTION.ordinal()));

        XSSFChart chart = createChart(sheet, name, 0, rowCount + 2, PROFIT_PROPORTION.ordinal() + 1, 30);
        XDDFChartData data = createPieChartData(chart);
        chart.getOrAddLegend().setPosition(LegendPosition.TOP);

        data.addSeries(securities, proportions);
        chart.plot(data);
    }
}
