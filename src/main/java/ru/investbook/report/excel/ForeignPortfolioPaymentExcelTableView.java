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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableHeader;
import ru.investbook.repository.PortfolioRepository;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static ru.investbook.report.excel.ForeignPortfolioPaymentExcelTableHeader.*;

@Component
public class ForeignPortfolioPaymentExcelTableView extends ExcelTableView {

    @Getter
    private final boolean summaryView = false;
    @Getter
    private final int sheetOrder = 4;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> portfolio + " (внешние выплаты)";

    public ForeignPortfolioPaymentExcelTableView(PortfolioRepository portfolioRepository,
                                                 ForeignPortfolioPaymentExcelTableFactory tableFactory,
                                                 PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    protected <T extends Enum<T> & TableHeader> void writeHeader(Sheet sheet, Class<T> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        sheet.setColumnWidth(CASH_RUB.ordinal(), 18 * 256);
        sheet.setColumnWidth(DESCRIPTION.ordinal(), 120 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record total = Table.newRecord();
        total.put(DATE, "Итого:");
        total.put(CASH_RUB, "=SUM(" + CASH_RUB.getRange(3, table.size() + 2) + ")/2");
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
