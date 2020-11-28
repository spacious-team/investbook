/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.vtb;

import org.spacious_team.broker.report_parser.api.InitializableReportTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collector;

import static java.util.Collections.unmodifiableCollection;
import static org.spacious_team.table_wrapper.api.AbstractTable.addWithEqualityChecker;

public abstract class AbstractVtbCashFlowTable<RowType> extends InitializableReportTable<RowType> {

    private final Collection<CashFlowEventTable.CashFlowEvent> events;

    public AbstractVtbCashFlowTable(CashFlowEventTable cashFlowEventTable) {
        super(cashFlowEventTable.getReport());
        this.events = unmodifiableCollection(cashFlowEventTable.getData());
    }

    @Override
    protected Collection<RowType> parseTable() {
        return events.stream()
                .flatMap(e -> getRow(e).stream())
                .collect(Collector.of(
                        ArrayList::new,
                        (collection, element) -> addWithEqualityChecker(
                                element,
                                collection,
                                this::checkEquality,
                                this::mergeDuplicates),
                        (left, right) -> {
                            left.addAll(right);
                            return left;
                        },
                        Collector.Characteristics.IDENTITY_FINISH
                ));
    }

    protected abstract Collection<RowType> getRow(CashFlowEventTable.CashFlowEvent event);

    protected boolean checkEquality(RowType object1, RowType object2) {
        return object1.equals(object2);
    }

    protected Collection<RowType> mergeDuplicates(RowType oldObject, RowType newObject) {
        return Arrays.asList(oldObject, newObject);
    }
}
