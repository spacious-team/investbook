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
import org.decampo.xirr.NewtonRaphson;
import org.decampo.xirr.Transaction;
import org.decampo.xirr.Xirr;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.Table;
import ru.investbook.report.TableHeader;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.service.AssetsAndCashService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static ru.investbook.report.excel.CashFlowExcelTableHeader.*;
import static ru.investbook.report.excel.ExcelFormulaHelper.xirr;

@Component
@Slf4j
public class CashFlowExcelTableView extends ExcelTableView {

    private final ForeignExchangeRateService foreignExchangeRateService;
    @Getter
    private final boolean summaryView = true;
    @Getter
    private final int sheetOrder = 9;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> "Доходность (" + portfolio + ")";
    private final AssetsAndCashService assetsAndCashService;
    private final Xirr.Builder xirrBuilder = Xirr.builder()
            .withNewtonRaphsonBuilder(NewtonRaphson.builder().withTolerance(0.001)); // in currency units (RUB, USD)

    public CashFlowExcelTableView(PortfolioRepository portfolioRepository,
                                  CashFlowExcelTableFactory tableFactory,
                                  PortfolioConverter portfolioConverter,
                                  AssetsAndCashService assetsAndCashService,
                                  ForeignExchangeRateService foreignExchangeRateService) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.assetsAndCashService = assetsAndCashService;
        this.foreignExchangeRateService = foreignExchangeRateService;
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(CASH.ordinal(), 17 * 256);
        sheet.setColumnWidth(CASH_RUB.ordinal(), 22 * 256);
        sheet.setColumnWidth(DAYS_COUNT.ordinal(), 18 * 256);
        sheet.setColumnWidth(DESCRIPTION.ordinal(), 50 * 256);
        sheet.setColumnWidth(LIQUIDATION_VALUE_RUB.ordinal(), 31 * 256);
        sheet.setColumnWidth(PROFIT.ordinal(), 28 * 256);
        sheet.setColumnWidth(CASH_BALANCE.ordinal(), 19 * 256);
        sheet.setColumnWidth(CURRENCY_NAME.ordinal(), 15 * 256);
        sheet.setColumnWidth(EXCHANGE_RATE.ordinal(), 15 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record total = Table.newRecord();
        String _portfolio = portfolio
                .orElseThrow(() -> new IllegalArgumentException("Ожидается портфель"))
                .getId();
        BigDecimal liquidationValueRub = assetsAndCashService.getTotalAssetsInRub(_portfolio).orElse(BigDecimal.ZERO);
        total.put(DATE, "Итого:");
        total.put(CASH_RUB, "=SUM(" +
                CASH_RUB.getRange(3, table.size() + 2) + ")+" +
                LIQUIDATION_VALUE_RUB.getCellAddr());
        total.put(LIQUIDATION_VALUE_RUB, liquidationValueRub);
        double xirrProfitForApachePoi = getXirrProfitInPercent(table, liquidationValueRub);
        total.put(PROFIT, xirr(CASH_RUB, DATE, 3, table.size() + 2, xirrProfitForApachePoi));
        total.put(CASH_BALANCE, "=SUMPRODUCT(" + CASH_BALANCE.getRange(3, table.size() + 2) + ","
                + EXCHANGE_RATE.getRange(3, table.size() + 2) + ")");
        return total;
    }

    private double getXirrProfitInPercent(Table table, BigDecimal liquidationValueRub) {
        try {
            Collection<Transaction> transactions = table.stream()
                    .map(this::castRecordToXirrTransaction)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(ArrayList::new));
            transactions.add(new Transaction(-liquidationValueRub.doubleValue(), LocalDate.now()));
            return xirrBuilder.withTransactions(transactions).xirr();
        } catch (Exception e) {
            log.debug("Can't calculate XIRR, set to 0", e);
            return 0;
        }
    }

    private Optional<Transaction> castRecordToXirrTransaction(Table.Record record) {
        Object cash = record.get(CASH);
        Object currency = record.get(CURRENCY);
        Double cashInRub = null;
        if (cash instanceof Number n && currency instanceof String cur) {
            cashInRub = n.doubleValue() * foreignExchangeRateService.getExchangeRateToRub(cur).doubleValue();
        }
        Object date = record.get(DATE);
        Transaction transaction = (cashInRub != null && date instanceof Instant instant) ?
                new Transaction(cashInRub, LocalDate.ofInstant(instant, ZoneId.systemDefault())) :
                null;
        return Optional.ofNullable(transaction);
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(DAYS_COUNT.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getIntStyle());
            }
            cell = row.getCell(DESCRIPTION.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == DAYS_COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
