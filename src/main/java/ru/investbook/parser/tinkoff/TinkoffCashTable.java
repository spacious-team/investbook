/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.math.BigDecimal;

public class TinkoffCashTable extends SingleAbstractReportTable<PortfolioCash>  {

    protected TinkoffCashTable(SingleBrokerReport report) {
        super(report,
                (cell) -> cell.startsWith("2. Операции с денежными средствами"),
                (cell) -> cell.contains("Дата"),
                CashTableHeader.class);
    }

    @Override
    protected PortfolioCash parseRow(TableRow row) {
        BigDecimal value = row.getBigDecimalCellValueOrDefault(CashTableHeader.VALUE, null);
        if (value == null) {
            return null;
        }
        return PortfolioCash.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().getReportEndDateTime())
                .market("all")
                .value(value)
                .currency(row.getStringCellValue(CashTableHeader.CURRENCY))
                .build();
    }

    @RequiredArgsConstructor
    protected enum CashTableHeader implements TableColumnDescription {
        CURRENCY("Валюта"),
        VALUE("Исходящий остаток", "на конец периода");

        @Getter
        private final TableColumn column;

        CashTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
