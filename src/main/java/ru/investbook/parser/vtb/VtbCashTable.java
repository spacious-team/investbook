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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public class VtbCashTable extends SingleAbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "Отчет об остатках денежных средств";
    private static final String TABLE_FOOTER = "Сумма денежных средств";


    protected VtbCashTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, TABLE_FOOTER, VtbCashTableHeader.class, 3);
    }

    @Override
    protected Collection<PortfolioCash> parseRowToCollection(TableRow row) {
        Collection<PortfolioCash> cashes = new ArrayList<>();
        cashes.addAll(getPortfolioCash(row, VtbCashTableHeader.STOCK_MARKET, "основной рынок"));
        cashes.addAll(getPortfolioCash(row, VtbCashTableHeader.FORTS_MARKET, "срочный рынок"));
        cashes.addAll(getPortfolioCash(row, VtbCashTableHeader.NON_MARKET, "внебирж. рынок"));
        return cashes;
    }

    private Collection<PortfolioCash> getPortfolioCash(TableRow row, VtbCashTableHeader column, String section) {
        try {
            return Collections.singleton(PortfolioCash.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(getReport().getReportEndDateTime())
                    .currency(VtbBrokerReport.convertToCurrency(row.getStringCellValue(VtbCashTableHeader.CURRENCY)))
                    .section(section)
                    .value(row.getBigDecimalCellValue(column))
                    .build());
        } catch (Exception e) {
            log.debug("Отсутствует значение колонки '{}' в таблице {}", column, row.getTable());
            return Collections.emptyList();
        }
    }

    @Getter
    @RequiredArgsConstructor
    private enum VtbCashTableHeader implements TableColumnDescription {
        CURRENCY(TableColumnImpl.of("Валюта")),
        STOCK_MARKET(OptionalTableColumn.of(
                MultiLineTableColumn.of("Исходящий остаток", "площадка", "основной рынок"))),
        FORTS_MARKET(OptionalTableColumn.of(
                MultiLineTableColumn.of("Исходящий остаток", "площадка", "срочный рынок"))),
        NON_MARKET(OptionalTableColumn.of(
                MultiLineTableColumn.of("Исходящий остаток", "площадка", "внебирж. рынок")));

        private final TableColumn column;
    }
}
