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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.ArrayList;
import java.util.Collection;

public class VtbCashTable extends AbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "Отчет об остатках денежных средств";
    private static final String TABLE_FOOTER = "Сумма денежных средств";


    protected VtbCashTable(BrokerReport report) {
        super(report, TABLE_NAME, TABLE_FOOTER, VtbCashTableHeader.class, 3);
    }

    @Override
    protected Collection<PortfolioCash> getRow(Table table, TableRow row) {
        Collection<PortfolioCash> cashes = new ArrayList<>();
        cashes.add(getPortfolioCash(table, row, VtbCashTableHeader.STOCK_MARKET, "основной рынок"));
        cashes.add(getPortfolioCash(table, row, VtbCashTableHeader.FORTS_MARKET, "срочный рынок"));
        cashes.add(getPortfolioCash(table, row, VtbCashTableHeader.NON_MARKET, "внебирж. рынок"));
        return cashes;
    }

    private PortfolioCash getPortfolioCash(Table table, TableRow row, VtbCashTableHeader column, String section) {
        return PortfolioCash.builder()
                .currency(VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, VtbCashTableHeader.CURRENCY)))
                .section(section)
                .value(table.getCurrencyCellValue(row, column))
                .build();
    }

    @Getter
    @RequiredArgsConstructor
    private enum VtbCashTableHeader implements TableColumnDescription {
        CURRENCY(TableColumnImpl.of("Валюта")),
        STOCK_MARKET(MultiLineTableColumn.of("Исходящий остаток", "площадка", "основной рынок")),
        FORTS_MARKET(MultiLineTableColumn.of("Исходящий остаток", "площадка", "срочный рынок")),
        NON_MARKET(MultiLineTableColumn.of("Исходящий остаток", "площадка", "внебирж. рынок"));

        private final TableColumn column;
    }
}
