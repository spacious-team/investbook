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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.Security;

import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.ISIN;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.NAME;

@Slf4j
public class PortfolioSecuritiesTable extends AbstractReportTable<Security> {
    private static final String TABLE_NAME = "СОСТОЯНИЕ ПОРТФЕЛЯ ЦЕННЫХ БУМАГ";
    private static final String TABLE_END_TEXT = "Итого:";

    public PortfolioSecuritiesTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, PortfolioSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(ExcelTable table, Row row) {
        return singletonList(Security.builder()
                        .isin(table.getStringCellValue(row, ISIN))
                        .name(table.getStringCellValue(row, NAME))
                        .build());
    }

    enum PortfolioSecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin");

        @Getter
        private final TableColumn column;
        PortfolioSecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
