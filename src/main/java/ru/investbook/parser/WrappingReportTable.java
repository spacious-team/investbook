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

import lombok.Getter;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WrappingReportTable<RowType> implements ReportTable<RowType> {
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<RowType> data;

    public WrappingReportTable(BrokerReport report, List<RowType> data) {
        this.report = report;
        this.data = Collections.unmodifiableList(data);
    }

    @SafeVarargs
    public static <T> WrappingReportTable<T> of(ReportTable<T>... tables) {
        Assert.isTrue(tables.length > 0, "Can't wrap, report tables not provided");
        BrokerReport report = tables[0].getReport();
        boolean isAllReportsIsSame = Arrays.stream(tables)
                .map(ReportTable::getReport)
                .filter(tableReport -> tableReport != report)
                .findAny()
                .isEmpty();
        Assert.isTrue(isAllReportsIsSame, "Wrapping report tables should be built for same broker report");
        return new WrappingReportTable<>(report, tables);

    }

    @SafeVarargs
    public WrappingReportTable(BrokerReport report, ReportTable<RowType>... tables) {
        List<RowType> data = new ArrayList<>();
        for (ReportTable<RowType> table : tables) {
            data.addAll(table.getData());
        }
        this.report = report;
        this.data = data;
    }
}
