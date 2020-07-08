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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ReportTableAdapter<RowType> implements ReportTable<RowType> {

    @Getter
    private final BrokerReport report;
    @Getter
    private final List<RowType> data = new ArrayList<>();

    protected ReportTableAdapter(ReportTable<RowType>... tables) {
        Set<BrokerReport> reports = Arrays.stream(tables).map(ReportTable::getReport).collect(Collectors.toSet());
        if (reports.size() == 1) {
            this.report = new ArrayList<>(reports).get(0);
        } else {
            throw new IllegalArgumentException("Can't wrap different report tables");
        }
        Arrays.asList(tables).forEach(r -> data.addAll(r.getData()));
    }
}
