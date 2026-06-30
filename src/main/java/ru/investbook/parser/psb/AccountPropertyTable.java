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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.AccountProperty;
import org.spacious_team.broker.pojo.AccountPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.ConstantPositionTableColumn;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.AccountPropertyTable.SummaryTableHeader.RUB;

@Slf4j
public class AccountPropertyTable extends SingleInitializableReportTable<AccountProperty> {
    public static final String SUMMARY_TABLE = "Сводная информация по счетам клиента в валюте счета";
    static final String ASSETS = "\"СУММА АКТИВОВ\" на конец дня";

    public AccountPropertyTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<AccountProperty> parseTable() {
        Table table = getSummaryTable();
        return getTotalAssets(table);
    }

    protected Table getSummaryTable() {
        return getSummaryTable(getReport(), ASSETS);
    }

    public static Table getSummaryTable(BrokerReport report, String tableFooterString) {
        Table table = report.getReportPage()
                .createTable(SUMMARY_TABLE, 1, tableFooterString, SummaryTableHeader.class, 1);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }

    protected Collection<AccountProperty> getTotalAssets(Table table) {
        try {
            @Nullable TableRow row = table.findRowByPrefix(ASSETS);
            if (row == null) {
                return emptyList();
            }
            return Collections.singletonList(AccountProperty.builder()
                    .account(getReport().getAccount())
                    .property(AccountPropertyType.TOTAL_ASSETS_RUB)
                    .value(row.getBigDecimalCellValue(RUB).toString())
                    .timestamp(getReport().getReportEndDateTime())
                    .build());
        } catch (Exception e) {
            log.info("Не могу получить стоимость активов из отчета {}", getReport());
            return emptyList();
        }
    }

    @Getter
    @RequiredArgsConstructor
    public enum SummaryTableHeader implements TableHeaderColumn {
        DESCRIPTION(1),
        RUB(AnyOfTableColumn.of(
                PatternTableColumn.of("RUB"),
                PatternTableColumn.of("RUR"))), // for fx market reports since 7.2021
        USD(OptionalTableColumn.of(PatternTableColumn.of("USD"))),
        EUR(OptionalTableColumn.of(PatternTableColumn.of("EUR"))),
        GBP(OptionalTableColumn.of(PatternTableColumn.of("GBP"))),
        CHF(OptionalTableColumn.of(PatternTableColumn.of("CHF")));

        private final TableColumn column;
        SummaryTableHeader(int columnIndex) {
            this.column = ConstantPositionTableColumn.of(columnIndex);
        }
    }
}
