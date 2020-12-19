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

import static ru.investbook.view.excel.ForeignMarketProfitExcelTableHeader.*;

@Component
public class ForeignMarketProfitExcelTableView extends ExcelTableView {

    @Getter
    private final int sheetOrder = 6;
    @Getter(AccessLevel.PROTECTED)
    private final UnaryOperator<String> sheetNameCreator = portfolio -> portfolio + " (валюта)";

    public ForeignMarketProfitExcelTableView(PortfolioRepository portfolioRepository,
                                             ForeignMarketProfitExcelTableFactory tableFactory,
                                             PortfolioConverter portfolioConverter) {
        super(portfolioRepository, tableFactory, portfolioConverter);
    }

    @Override
    protected void writeHeader(Sheet sheet, Class<? extends TableHeader> headerType, CellStyle style) {
        super.writeHeader(sheet, headerType, style);
        for (TableHeader header : headerType.getEnumConstants()) {
            sheet.setColumnWidth(header.ordinal(), 18 * 256);
        }
        sheet.setColumnWidth(CURRENCY_PAIR.ordinal(), 20 * 256);
    }

    @Override
    protected Table.Record getTotalRow(Table table, Optional<Portfolio> portfolio) {
        Table.Record totalRow = new Table.Record();
        for (ForeignMarketProfitExcelTableHeader column : ForeignMarketProfitExcelTableHeader.values()) {
            totalRow.put(column, "=SUM(" + column.getRange(3, table.size() + 2) + ")");
        }
        totalRow.put(CURRENCY_PAIR, "Итого:");
        totalRow.put(COUNT, "=SUMPRODUCT(ABS(" + COUNT.getRange(3, table.size() + 2) + "))");
        totalRow.remove(OPEN_DATE);
        totalRow.remove(CLOSE_DATE);
        totalRow.remove(OPEN_PRICE);
        totalRow.remove(YIELD);
        return totalRow;
    }

    @Override
    protected void sheetPostCreate(Sheet sheet, Class<? extends TableHeader> headerType, CellStyles styles) {
        super.sheetPostCreate(sheet, headerType, styles);
        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue;
            Cell cell = row.getCell(CURRENCY_PAIR.ordinal());
            if (cell != null) {
                cell.setCellStyle(styles.getLeftAlignedTextStyle());
            }
        }
        for (Cell cell : sheet.getRow(1)) {
            if (cell == null) continue;
            if (cell.getColumnIndex() == CURRENCY_PAIR.ordinal()) {
                cell.setCellStyle(styles.getTotalTextStyle());
            } else if (cell.getColumnIndex() == COUNT.ordinal()){
                cell.setCellStyle(styles.getIntStyle());
            } else {
                cell.setCellStyle(styles.getTotalRowStyle());
            }
        }
    }
}
