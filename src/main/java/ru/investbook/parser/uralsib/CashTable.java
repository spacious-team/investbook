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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import static ru.investbook.parser.uralsib.CashTable.CashTableHeader.CURRENCY;
import static ru.investbook.parser.uralsib.CashTable.CashTableHeader.VALUE;

@Slf4j
public class CashTable extends SingleAbstractReportTable<PortfolioCash> {

    private static final String TABLE_NAME = "ПОЗИЦИЯ ПО ДЕНЕЖНЫМ СРЕДСТВАМ";

    public CashTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", CashTableHeader.class, 2);
    }

    @Override
    protected PortfolioCash parseRow(TableRow row) {
        return PortfolioCash.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().getReportEndDateTime())
                .market("all")
                .value(row.getBigDecimalCellValue(VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(row.getStringCellValue(CURRENCY)))
                .build();
    }

    enum CashTableHeader implements TableHeaderColumn {
        VALUE("исходящий остаток"),
        CURRENCY("код валюты");

        @Getter
        private final TableColumn column;
        CashTableHeader(String ... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
