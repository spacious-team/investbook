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

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static ru.investbook.view.excel.DerivativesMarketProfitExcelTableHeader.*;

@Component
public class DerivativesMarketProfitExcelTableView extends ExcelTableView {

    @Getter
    private final int sheetOrder = 5;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> portfolio + " (срочный)";

    public DerivativesMarketProfitExcelTableView(PortfolioRepository portfolioRepository,
                                                 DerivativesMarketProfitExcelTableFactory tableFactory,
                                                 PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(CONTRACT.ordinal(), 24 * 256);
        sheet.setColumnWidth(AMOUNT.ordinal(), 16 * 256);
        sheet.setColumnWidth(DERIVATIVE_PROFIT_DAY.ordinal(), 17 * 256);
        sheet.setColumnWidth(DERIVATIVE_PROFIT_TOTAL.ordinal(), 17 * 256);
        sheet.setColumnWidth(POSITION.ordinal(), 17 * 256);
        sheet.setColumnWidth(FORECAST_TAX.ordinal(), 17 * 256);
        sheet.setColumnWidth(PROFIT.ordinal(), 17 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record totalRow = new Table.Record();
        totalRow.put(CONTRACT, "Итого:");
        totalRow.put(COUNT, getSumFormula(COUNT, table.size()));
        totalRow.put(AMOUNT, "=SUMPRODUCT(ABS(" + AMOUNT.getRange(3, table.size() + 2) + "))");
        totalRow.put(COMMISSION, getSumFormula(COMMISSION, table.size()) + "/2");
        totalRow.put(DERIVATIVE_PROFIT_DAY, getSumFormula(DERIVATIVE_PROFIT_DAY, table.size()));
        String profitMinusCommission = "(" + DERIVATIVE_PROFIT_DAY.getCellAddr() + "-" + COMMISSION.getCellAddr() + ")";
        totalRow.put(FORECAST_TAX, "=IF(" + profitMinusCommission + "<=0,0,0.13*" + profitMinusCommission +")");
        totalRow.put(PROFIT, "=" + DERIVATIVE_PROFIT_DAY.getCellAddr()
                + "-" + COMMISSION.getCellAddr()
                + "-" + FORECAST_TAX.getCellAddr());
        return totalRow;
    }

    private String getSumFormula(DerivativesMarketProfitExcelTableHeader column, int tableSize) {
        return "=SUM(" + column.getRange(3, tableSize + 2) + ")";
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(CONTRACT.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == CONTRACT.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
