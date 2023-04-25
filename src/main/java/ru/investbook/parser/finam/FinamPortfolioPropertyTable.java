/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.finam;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.ConstantPositionTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.util.Collection;
import java.util.Collections;

@Slf4j
public class FinamPortfolioPropertyTable  extends SingleInitializableReportTable<PortfolioProperty> {

    private static final String SUMMARY_TABLE = "1. Оценка состояния счета Клиента";
    private static final String ASSETS = "ИТОГО оценка состояния счета (руб.)";

    public FinamPortfolioPropertyTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        final Table table = getSummaryTable();
        return getTotalAssets(table);
    }

    protected Table getSummaryTable() {
        return getSummaryTable(getReport(), ASSETS);
    }

    public static Table getSummaryTable(BrokerReport report, String tableFooterString) {
        final Table table = report.getReportPage()
                .createNameless("На начало периода", tableFooterString, FinamSummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + SUMMARY_TABLE + "' не найдена");
        }
        return table;
    }

    protected Collection<PortfolioProperty> getTotalAssets(Table table) {
        try {
            final TableRow row = table.findRowByPrefix(ASSETS);
            if (row == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                    .value(row.getBigDecimalCellValue(FinamSummaryTableHeader.PERIOD_END).toString())
                    .timestamp(getReport().getReportEndDateTime())
                    .build()
            );
        } catch (Exception e) {
            log.info("Не могу получить стоимость активов из отчета {}", getReport());
            return Collections.emptyList();
        }
    }

    @RequiredArgsConstructor
    private enum FinamSummaryTableHeader implements TableHeaderColumn {
        DESCRIPTION(ConstantPositionTableColumn.of(0)),
        PERIOD_BEGIN(PatternTableColumn.of(HeaderDescriptions.PERIOD_BEGIN_HEADER)),
        PERIOD_END(PatternTableColumn.of(HeaderDescriptions.PERIOD_END_HEADER)),
        PERIOD_CHANGE(PatternTableColumn.of(HeaderDescriptions.PERIOD_CHANGE_HEADER));

        @Getter
        private final TableColumn column;

        private static class HeaderDescriptions {
            private static final String PERIOD_BEGIN_HEADER = "На начало периода";
            private static final String PERIOD_END_HEADER = "На конец периода";
            private static final String PERIOD_CHANGE_HEADER = "Изменение за период";
        }

    }
}
