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

package ru.investbook.parser.investbook;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.ConstantPositionTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;

public class AbstractInvestbookTable<RowType> extends AbstractReportTable<RowType> {

    protected AbstractInvestbookTable(BrokerReport report) {
        super(report,
                "Investbook Report Table",
                "Дата",
                null,
                InvestbookReportTableHeader.class);
    }


    @Getter
    @RequiredArgsConstructor
    public enum InvestbookReportTableHeader implements TableHeaderColumn {
        OPERATION(AnyOfTableColumn.of(
                PatternTableColumn.of("событие"),
                PatternTableColumn.of("тип"),
                ConstantPositionTableColumn.of(0))),
        PORTFOLIO(AnyOfTableColumn.of(
                PatternTableColumn.of("счет"),
                ConstantPositionTableColumn.of(1))),
        DATE_TIME(AnyOfTableColumn.of(
                PatternTableColumn.of("дата"),
                ConstantPositionTableColumn.of(2))),
        TICKER_NAME_ISIN(AnyOfTableColumn.of(
                PatternTableColumn.of("наименование"),
                ConstantPositionTableColumn.of(3))),
        COUNT(AnyOfTableColumn.of(
                PatternTableColumn.of("количество"),
                ConstantPositionTableColumn.of(4))),
        PRICE(AnyOfTableColumn.of(
                PatternTableColumn.of("цена"),
                ConstantPositionTableColumn.of(5))),
        ACCRUED_INTEREST(AnyOfTableColumn.of(
                PatternTableColumn.of("нкд"),
                ConstantPositionTableColumn.of(6))),
        DERIVATIVE_PRICE_IN_CURRENCY(AnyOfTableColumn.of(
                PatternTableColumn.of("стоимость", "контракта"),
                ConstantPositionTableColumn.of(7))),
        CURRENCY(AnyOfTableColumn.of(
                PatternTableColumn.of("валюта", "цены"),
                ConstantPositionTableColumn.of(8))),
        FEE(AnyOfTableColumn.of(
                PatternTableColumn.of("комиссия"),
                ConstantPositionTableColumn.of(9))),
        FEE_CURRENCY(AnyOfTableColumn.of(
                PatternTableColumn.of("валюта", "комиссии"),
                ConstantPositionTableColumn.of(10)));

        private final TableColumn column;
    }
}
