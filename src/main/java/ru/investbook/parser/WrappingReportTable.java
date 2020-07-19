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

import java.util.ArrayList;
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

    public WrappingReportTable(BrokerReport report, ReportTable<RowType>... tables) {
        List<RowType> data = new ArrayList<>();
        for (ReportTable<RowType> table : tables) {
            data.addAll(table.getData());
        }
        this.report = report;
        this.data = data;
    }
}
