/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;
import org.spacious_team.broker.report_parser.api.TableFactoryRegistry;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableFactory;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.uralsib.PortfolioPropertyTable.SummaryTableHeader.RUB;

@Slf4j
public class PortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {
    private static final String ASSETS_TABLE = "ОЦЕНКА АКТИВОВ";
    private static final String TABLE_FIRST_HEADER_LINE = "На конец отчетного периода";
    private static final String TABLE_SECOND_HEADER_LINE = "по цене закрытия";
    private static final String ASSETS = "Общая стоимость активов:";

    protected PortfolioPropertyTable(UralsibBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        try {
            BrokerReport report = getReport();
            ReportPage reportPage = report.getReportPage();
            TableFactory tableFactory = TableFactoryRegistry.get(reportPage);
            Table table = tableFactory.createOfNoName(reportPage, ASSETS_TABLE, TABLE_FIRST_HEADER_LINE,
                    SummaryTableHeader.class, 3);
            if (table.isEmpty()) {
                table = tableFactory.createOfNoName(reportPage, ASSETS_TABLE, TABLE_SECOND_HEADER_LINE,
                        SummaryTableHeader.class, 2);
            }

            PortfolioProperty.PortfolioPropertyBuilder propertyBuilder = PortfolioProperty.builder()
                    .portfolio(report.getPortfolio())
                    .timestamp(report.getReportEndDateTime())
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB);

            if (table.isEmpty()) {
                log.info("Таблица '{}' не найдена, считаю, что активы равны 0", ASSETS_TABLE);
                return singletonList(propertyBuilder
                        .value("0")
                        .build());
            }
            TableRow row = table.findRow(ASSETS);
            if (row == null) {
                return emptyList();
            }
            return singletonList(propertyBuilder
                    .value(table.getCurrencyCellValue(row, RUB).toString())
                    .build());
        } catch (Exception e) {
            log.info("Не могу распарсить таблицу '{}' в файле {}", ASSETS_TABLE, getReport().getPath().getFileName(), e);
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
