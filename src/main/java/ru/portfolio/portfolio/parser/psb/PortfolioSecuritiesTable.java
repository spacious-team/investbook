/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.portfolio.portfolio.parser.ExcelTableHelper.rowContains;
import static ru.portfolio.portfolio.parser.psb.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.*;

@Slf4j
public class PortfolioSecuritiesTable extends AbstractReportTable<PortfolioSecuritiesTable.PortfolioSecuritiesTableRow> {
    private static final String TABLE_NAME = "Портфель на конец дня на биржевом рынке";
    private static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    private static final String INVALID_TEXT = "Итого в валюте цены";

    public PortfolioSecuritiesTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, PortfolioSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<PortfolioSecuritiesTableRow> getRow(ExcelTable table, Row row) {
        return rowContains(table.getSheet(), row.getRowNum(), INVALID_TEXT) ?
                emptyList() :
                getPosition(table, row);
    }

    private static Collection<PortfolioSecuritiesTableRow> getPosition(ExcelTable table, Row row) {
        String currency = table.getStringCellValue(row, CURRENCY);
        return Collections.singletonList(PortfolioSecuritiesTableRow.builder()
                .name(table.getStringCellValue(row, NAME))
                .isin(table.getStringCellValue(row, ISIN))
                .buyCount(table.getIntCellValue(row, BUY))
                .cellCount(table.getIntCellValue(row, CELL))
                .outgoingCount(table.getIntCellValue(row, OUTGOING))
                .currency((currency != null && !currency.isEmpty()) ? currency : "RUB")
                .amount(table.getCurrencyCellValue(row, AMOUNT))
                .accruedInterest(table.getCurrencyCellValue(row, ACCRUED_INTEREST))
                .build());
    }

    enum PortfolioSecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin"),
        OUTGOING("исходящий", "остаток"),
        BUY("зачислено"),
        CELL("списано"),
        AMOUNT("оценочная стоимость в валюте цены"),
        ACCRUED_INTEREST("нкд"),
        CURRENCY("валюта цены");

        @Getter
        private final TableColumn column;
        PortfolioSecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @Builder
    public static class PortfolioSecuritiesTableRow {
        private String isin;
        private String name;
        private int buyCount;
        private int cellCount;
        private int outgoingCount;
        private BigDecimal amount; // оценочная стоиомсть в валюце цены
        private BigDecimal accruedInterest; // НКД, в валюте бумаги
        private String currency; // валюта
    }
}
