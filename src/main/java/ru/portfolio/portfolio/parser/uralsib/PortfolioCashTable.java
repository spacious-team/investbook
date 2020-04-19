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

import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioCashTable.CashTableHeader.CURRENCY;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioCashTable.CashTableHeader.VALUE;

@Slf4j
public class PortfolioCashTable extends AbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "ПОЗИЦИЯ ПО ДЕНЕЖНЫМ СРЕДСТВАМ";

    public PortfolioCashTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", CashTableHeader.class, 2);
    }

    @Override
    protected Collection<PortfolioCash> getRow(ExcelTable table, Row row) {
        return singletonList(PortfolioCash.builder()
                .section("all")
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .build());
    }

    enum CashTableHeader implements TableColumnDescription {
        VALUE("исходящий остаток"),
        CURRENCY("код валюты");

        @Getter
        private final TableColumn column;
        CashTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
