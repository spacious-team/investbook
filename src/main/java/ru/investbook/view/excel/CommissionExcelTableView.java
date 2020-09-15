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

import static ru.investbook.view.excel.CommissionExcelTableHeader.*;

@Component
public class CommissionExcelTableView extends ExcelTableView {

    public CommissionExcelTableView(PortfolioRepository portfolioRepository,
                                    CommissionExcelTableFactory tableFactory,
                                    PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(COMMISSION.ordinal(), 30 * 256);
        sheet.setColumnWidth(DESCRIPTION.ordinal(), 75 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table) {
        Table.Record total = Table.newRecord();
        total.put(DATE, "Итого:");
        total.put(COMMISSION, "=SUM(" + COMMISSION.getRange(3, table.size() + 2) + ")");
        return total;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(DESCRIPTION.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == DATE.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
