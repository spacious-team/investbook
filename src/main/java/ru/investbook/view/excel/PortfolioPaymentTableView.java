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
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableHeader;

import static ru.investbook.view.excel.PortfolioPaymentTableHeader.*;

@Component
public class PortfolioPaymentTableView extends ExcelTableView {

    public PortfolioPaymentTableView(PortfolioRepository portfolioRepository,
                                     PortfolioPaymentTableFactory tableFactory,
                                     PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(SECURITY.ordinal(), 45 * 256);
        sheet.setColumnWidth(CASH_RUB.ordinal(), 18 * 256);
        sheet.setColumnWidth(PAYMENT_TYPE.ordinal(), 23 * 256);
    }

    @Override
    protected Table.Record getTotalRow() {
        Table.Record total = Table.newRecord();
        total.put(SECURITY, "Итого:");
        total.put(COUNT, "=SUM(" +
                COUNT.getColumnIndex() + "3:" +
                COUNT.getColumnIndex() + "100000)");
        total.put(CASH_RUB, "=SUM(" +
                CASH_RUB.getColumnIndex() + "3:" +
                CASH_RUB.getColumnIndex() + "100000)/2");
        return total;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, CellStyles styles) {
        super.sheetPostCreate(sheet, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(SECURITY.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
            cell = row.getCell(PAYMENT_TYPE.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == SECURITY.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()) {
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
