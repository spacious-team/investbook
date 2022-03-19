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

package ru.investbook.parser;

import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.TableColumnDescription;

import java.time.Instant;
import java.util.function.Predicate;

/**
 * Report table reading information from {@link SingleBrokerReport}
 */
public abstract class SingleAbstractReportTable<T>  extends AbstractReportTable<T> {

    protected SingleAbstractReportTable(SingleBrokerReport report, String tableName, String tableFooter,
                                        Class<? extends TableColumnDescription> headerDescription) {
        super(report, tableName, tableFooter, headerDescription);
    }

    protected SingleAbstractReportTable(SingleBrokerReport report, String tableName, String tableFooter,
                                        Class<? extends TableColumnDescription> headerDescription, int headersRowCount) {
        super(report, tableName, tableFooter, headerDescription, headersRowCount);
    }

    protected SingleAbstractReportTable(SingleBrokerReport report, Predicate<String> tableNameFinder,
                                        Predicate<String> tableFooterFinder,
                                        Class<? extends TableColumnDescription> headerDescription) {
        super(report, tableNameFinder, tableFooterFinder, headerDescription);
    }

    protected SingleAbstractReportTable(SingleBrokerReport report, Predicate<String> tableNameFinder,
                                        Predicate<String> tableFooterFinder,
                                        Class<? extends TableColumnDescription> headerDescription, int headersRowCount) {
        super(report, tableNameFinder, tableFooterFinder, headerDescription, headersRowCount);
    }

    public SingleAbstractReportTable(SingleBrokerReport report, String providedTableName, String namelessTableFirstLine,
                                     String tableFooter, Class<? extends TableColumnDescription> headerDescription) {
        super(report, providedTableName, namelessTableFirstLine, tableFooter, headerDescription);
    }

    public SingleAbstractReportTable(SingleBrokerReport report, String providedTableName, String namelessTableFirstLine,
                                     String tableFooter, Class<? extends TableColumnDescription> headerDescription,
                                     int headersRowCount) {
        super(report, providedTableName, namelessTableFirstLine, tableFooter, headerDescription, headersRowCount);
    }

    protected SingleAbstractReportTable(SingleBrokerReport report, String providedTableName,
                                        Predicate<String> namelessTableFirstLineFinder, Predicate<String> tableFooterFinder,
                                        Class<? extends TableColumnDescription> headerDescription) {
        super(report, providedTableName, namelessTableFirstLineFinder, tableFooterFinder, headerDescription);
    }

    protected SingleAbstractReportTable(SingleBrokerReport report, String providedTableName,
                                        Predicate<String> namelessTableFirstLineFinder, Predicate<String> tableFooterFinder,
                                        Class<? extends TableColumnDescription> headerDescription, int headersRowCount) {
        super(report, providedTableName, namelessTableFirstLineFinder, tableFooterFinder, headerDescription, headersRowCount);
    }

    @Override
    public SingleBrokerReport getReport() {
        return (SingleBrokerReport) super.getReport();
    }

    protected Instant convertToInstant(String dateTime) {
        return getReport().convertToInstant(dateTime);
    }
}
