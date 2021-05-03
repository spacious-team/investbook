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
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableHeader;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static ru.investbook.report.excel.CashFlowExcelTableHeader.*;

@Component
public class CashFlowExcelTableView extends ExcelTableView {

    @Getter
    private final int sheetOrder = 8;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> "Доходность (" + portfolio + ")";
    private final PortfolioPropertyRepository portfolioPropertyRepository;

    public CashFlowExcelTableView(PortfolioRepository portfolioRepository,
                                  CashFlowExcelTableFactory tableFactory,
                                  PortfolioConverter portfolioConverter,
                                  PortfolioPropertyRepository portfolioPropertyRepository) {
        super(portfolioRepository, tableFactory, portfolioConverter);
        this.portfolioPropertyRepository = portfolioPropertyRepository;
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
        BigDecimal liquidationValueRub = portfolioPropertyRepository
                .findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(
                        portfolio
                                .orElseThrow(() -> new IllegalArgumentException("Ожидается портфель"))
                                .getId(),
                        PortfolioPropertyType.TOTAL_ASSETS_RUB.name()) // TODO sum with TOTAL_ASSETS_USD div by USDRUB exchange rate
                .map(e -> BigDecimal.valueOf(Double.parseDouble(e.getValue())))
                .orElse(BigDecimal.ZERO);
        total.put(DATE, "Итого:");
        total.put(CASH_RUB, "=SUM(" +
                CASH_RUB.getRange(3, table.size() + 2) + ")+" +
                LIQUIDATION_VALUE_RUB.getCellAddr());
        total.put(LIQUIDATION_VALUE_RUB, liquidationValueRub);
        total.put(PROFIT, "=100*XIRR("
                + CASH_RUB.getRange(3, table.size() + 2) + ","
                + DATE.getRange(3, table.size() + 2) + ")");
        total.put(CASH_BALANCE, "=SUMPRODUCT(" + CASH_BALANCE.getRange(3, table.size() + 2) + ","
                + EXCHANGE_RATE.getRange(3, table.size() + 2) + ")");
        return total;
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
            if (cell.getColumnIndex() == DATE.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == DAYS_COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
