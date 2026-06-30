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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.AccountCash;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Slf4j
public class VtbCashTable extends SingleAbstractReportTable<AccountCash> {

    private static final String TABLE_NAME = "Отчет об остатках денежных средств";
    private static final String TABLE_NAME_WITH_YO = "Отчёт об остатках денежных средств";
    private static final String TABLE_FOOTER = "Сумма денежных средств";


    protected VtbCashTable(SingleBrokerReport report) {
        super(report,
                cell -> cell.startsWith(TABLE_NAME) || cell.startsWith(TABLE_NAME_WITH_YO),
                1,
                cell -> Objects.equals(cell, "RUR"),
                cell -> cell.startsWith(TABLE_FOOTER),
                VtbCashTableHeader.class);
    }

    @Override
    protected Collection<AccountCash> parseRowToCollection(TableRow row) {
        Collection<AccountCash> cashes = new ArrayList<>();
        cashes.addAll(getAccountCash(row, VtbCashTableHeader.STOCK_MARKET, "основной рынок"));
        cashes.addAll(getAccountCash(row, VtbCashTableHeader.FORTS_MARKET, "срочный рынок"));
        cashes.addAll(getAccountCash(row, VtbCashTableHeader.NON_MARKET, "внебирж. рынок"));
        return cashes;
    }

    private Collection<AccountCash> getAccountCash(TableRow row, VtbCashTableHeader column, String section) {
        try {
            return Collections.singleton(AccountCash.builder()
                    .account(getReport().getAccount())
                    .timestamp(getReport().getReportEndDateTime())
                    .currency(VtbBrokerReport.convertToCurrency(row.getStringCellValue(VtbCashTableHeader.CURRENCY)))
                    .market(section)
                    .value(row.getBigDecimalCellValue(column))
                    .build());
        } catch (Exception e) {
            log.debug("Отсутствует значение колонки '{}' в таблице {}", column, row.getTable());
            return Collections.emptyList();
        }
    }

    @Getter
    @RequiredArgsConstructor
    private enum VtbCashTableHeader implements TableHeaderColumn {
        CURRENCY(PatternTableColumn.of("Валюта")),
        STOCK_MARKET(
                OptionalTableColumn.of(
                        AnyOfTableColumn.of(
                                MultiLineTableColumn.of("Исходящий остаток", "основной рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "", "основной рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "площадка", "основной рынок")))),
        FORTS_MARKET(
                OptionalTableColumn.of(
                        AnyOfTableColumn.of(
                                MultiLineTableColumn.of("Исходящий остаток", "срочный рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "", "срочный рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "площадка", "срочный рынок")))),
        NON_MARKET(
                OptionalTableColumn.of(
                        AnyOfTableColumn.of(
                                MultiLineTableColumn.of("Исходящий остаток", "внебирж. рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "", "внебирж. рынок"),
                                MultiLineTableColumn.of("Исходящий остаток", "площадка", "внебирж. рынок"))));

        private final TableColumn column;
    }
}
