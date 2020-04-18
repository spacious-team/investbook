/*
 * Portfolio
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

package ru.portfolio.portfolio.parser;

import lombok.Getter;
import org.apache.poi.ss.usermodel.Row;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractReportTable<RowType> implements ReportTable<RowType> {
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<RowType> data = new ArrayList<>();

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
        this.report = report;
        ExcelTable table = (tableFooter != null && !tableFooter.isEmpty()) ?
                ExcelTable.of(report.getSheet(), tableName, tableFooter, headerDescription, headersRowCount) :
                ExcelTable.of(report.getSheet(), tableName, headerDescription, headersRowCount);
        this.data.addAll(pasreTable(table));
    }

    protected Collection<RowType> pasreTable(ExcelTable table) {
        return table.getDataCollection(getReport().getPath(), this::getRow);
    }

    protected Instant convertToInstant(String dateTime) {
        return report.convertToInstant(dateTime);
    }

    protected abstract Collection<RowType> getRow(ExcelTable table, Row row);
}
