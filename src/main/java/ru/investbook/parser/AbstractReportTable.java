/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser;

import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.table.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
public abstract class AbstractReportTable<RowType> extends InitializableReportTable<RowType> {

    private final String tableName;
    private final String tableFooter;
    private final Class<? extends TableColumnDescription> headerDescription;
    private final int headersRowCount;

    protected AbstractReportTable(BrokerReport report,
                                  String tableName,
                                  String tableFooter,
                                  Class<? extends TableColumnDescription> headerDescription) {
        this(report, tableName, tableFooter, headerDescription, 1);
    }

    protected AbstractReportTable(BrokerReport report,
                                  String tableName,
                                  String tableFooter,
                                  Class<? extends TableColumnDescription> headerDescription,
                                  int headersRowCount) {
        super(report);
        this.tableName = tableName;
        this.tableFooter = tableFooter;
        this.headerDescription = headerDescription;
        this.headersRowCount = headersRowCount;
    }

    @Override
    protected Collection<RowType> parseTable() {
        try {
            ReportPage reportPage = getReport().getReportPage();
            TableFactory tableFactory = TableFactoryRegistry.get(reportPage);
            Table table = (tableFooter != null && !tableFooter.isEmpty()) ?
                    tableFactory.create(reportPage, tableName, tableFooter, headerDescription, headersRowCount) :
                    tableFactory.create(reportPage, tableName, headerDescription, headersRowCount);
            return parseTable(table);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при парсинге таблицы '" + this.tableName + "' " +
                    "в файле " + getReport().getPath().getFileName(), e);
        }
    }

    protected Collection<RowType> parseTable(Table table) {
        return table.getDataCollection(getReport().getPath(), this::getRow, this::checkEquality, this::mergeDuplicates);
    }

    protected Instant convertToInstant(String dateTime) {
        return getReport().convertToInstant(dateTime);
    }

    protected abstract Collection<RowType> getRow(Table table, TableRow row);

    protected boolean checkEquality(RowType object1, RowType object2) {
        return object1.equals(object2);
    }

    protected Collection<RowType> mergeDuplicates(RowType oldObject, RowType newObject) {
        return Arrays.asList(oldObject, newObject);
    }
}
