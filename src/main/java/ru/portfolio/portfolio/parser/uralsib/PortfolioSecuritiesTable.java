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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.*;

@Slf4j
public class PortfolioSecuritiesTable extends AbstractReportTable<Map.Entry<Security, Integer>> {
    static final String TABLE_NAME = "СОСТОЯНИЕ ПОРТФЕЛЯ ЦЕННЫХ БУМАГ";
    static final String TABLE_END_TEXT = "Итого:";

    public PortfolioSecuritiesTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, PortfolioSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Map.Entry<Security, Integer>> getRow(ExcelTable table, Row row) {
        return singletonList(new AbstractMap.SimpleEntry<>(
                Security.builder()
                        .isin(table.getStringCellValue(row, ISIN))
                        .name(table.getStringCellValue(row, NAME))
                        .build(),
                table.getIntCellValue(row, INCOMING_COUNT)));
    }

    enum PortfolioSecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin"),
        INCOMING_COUNT("количество", "на начало периода");

        @Getter
        private final TableColumn column;
        PortfolioSecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
