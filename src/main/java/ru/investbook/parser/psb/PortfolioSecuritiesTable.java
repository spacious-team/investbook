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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.parser.TableColumn;
import ru.investbook.parser.TableColumnDescription;
import ru.investbook.parser.TableColumnImpl;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.pojo.Security;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.ISIN;
import static ru.investbook.parser.psb.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.NAME;

@Slf4j
public class PortfolioSecuritiesTable extends AbstractReportTable<Security> {
    private static final String TABLE_NAME = "Портфель на конец дня на биржевом рынке";
    private static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    private static final String INVALID_TEXT = "Итого в валюте цены";

    public PortfolioSecuritiesTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, PortfolioSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(Table table, TableRow row) {
        return row.rowContains(INVALID_TEXT) ?
                emptyList() :
                Collections.singletonList(Security.builder()
                        .isin(table.getStringCellValue(row, ISIN))
                        .name(table.getStringCellValue(row, NAME))
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
}
