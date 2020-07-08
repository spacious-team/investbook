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
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.pojo.PortfolioPropertyType;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import java.math.BigDecimal;

import static ru.investbook.view.excel.CashFlowExcelTableHeader.*;

@Component
public class CashFlowExcelTableView extends ExcelTableView {
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
        sheet.setColumnWidth(CURRENCY_NAME.ordinal(), 15 * 256);
        sheet.setColumnWidth(EXCHANGE_RATE.ordinal(), 15 * 256);
    }

    @Override
    protected Table.Record getTotalRow() {
        Table.Record total = Table.newRecord();
        total.put(DATE, "Итого:");
        total.put(CASH_RUB, "=SUM(" +
                CASH_RUB.getColumnIndex() + "3:" +
                CASH_RUB.getColumnIndex() + "100000)");
        total.put(LIQUIDATION_VALUE_RUB, portfolioPropertyRepository
                .findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(
                        getPortfolio().getId(),
                        PortfolioPropertyType.TOTAL_ASSETS.name())
                .map(e -> BigDecimal.valueOf(Double.parseDouble(e.getValue())))
                .orElse(BigDecimal.ZERO));
        total.put(PROFIT, "=(" + LIQUIDATION_VALUE_RUB.getColumnIndex() + "2-" + CASH_RUB.getColumnIndex() + "2)"
                + "/SUMPRODUCT("
                + CASH_RUB.getColumnIndex() + "3:" + CASH_RUB.getColumnIndex() + "100000,"
                + DAYS_COUNT.getColumnIndex() + "3:" + DAYS_COUNT.getColumnIndex() + "100000)*365*100");
        return total;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
        super.sheetPostCreate(sheet, styles);
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
