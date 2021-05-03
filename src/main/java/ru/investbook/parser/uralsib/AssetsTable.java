/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.uralsib.AssetsTable.SummaryTableHeader.RUB;

/**
 * Shows total assets value (sum of cash and security), but Assets table is empty if no security in portfolio.
 * In that case assets should be calculated by {@link CashTable}.
 */
@Slf4j
public class AssetsTable extends SingleInitializableReportTable<PortfolioProperty> {
    private static final String ASSETS_TABLE = "ОЦЕНКА АКТИВОВ";
    private static final String TABLE_FIRST_HEADER_LINE = "На конец отчетного периода";
    private static final String TABLE_SECOND_HEADER_LINE = "по цене закрытия";
    private static final String ASSETS = "Общая стоимость активов:";

    protected AssetsTable(UralsibBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        try {
            SingleBrokerReport report = getReport();
            Table table = report.getReportPage()
                    .createNameless(ASSETS_TABLE, TABLE_FIRST_HEADER_LINE, SummaryTableHeader.class, 3);
            if (table.isEmpty()) {
                table = report.getReportPage()
                        .createNameless(ASSETS_TABLE, TABLE_SECOND_HEADER_LINE, SummaryTableHeader.class, 2);
            }
            if (table.isEmpty()) {
                log.debug("Таблица '{}' не найдена", ASSETS_TABLE);
                return emptyList();
            }

            TableRow row = table.stream()
                    .filter(tableRow -> tableRow.rowContains(ASSETS))
                    .findAny()
                    .orElse(null);

            return (row == null) ? emptyList() :
                    singletonList(PortfolioProperty.builder()
                            .portfolio(report.getPortfolio())
                            .timestamp(report.getReportEndDateTime())
                            .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                            .value(row.getBigDecimalCellValue(RUB).toString())
                            .build());
        } catch (Exception e) {
            log.info("Не могу распарсить таблицу '{}' из отчета {}", ASSETS_TABLE, getReport(), e);
            return emptyList();
        }
    }

    @RequiredArgsConstructor
    enum SummaryTableHeader implements TableColumnDescription {
        RUB(AnyOfTableColumn.of(
                MultiLineTableColumn.of(
                        TableColumnImpl.of(TABLE_FIRST_HEADER_LINE),
                        TableColumnImpl.of(TABLE_SECOND_HEADER_LINE),
                        TableColumnImpl.of("RUR")),
                MultiLineTableColumn.of(
                        TableColumnImpl.of(TABLE_SECOND_HEADER_LINE),
                        TableColumnImpl.of("RUR"))));

        @Getter
        private final TableColumn column;
    }
}
