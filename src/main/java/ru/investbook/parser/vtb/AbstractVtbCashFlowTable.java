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

import ru.investbook.parser.SingleInitializableReportTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableCollection;
import static org.spacious_team.table_wrapper.api.AbstractTable.addWithEqualityChecker;

public abstract class AbstractVtbCashFlowTable<RowType> extends SingleInitializableReportTable<RowType> {

    private final Collection<CashFlowEventTable.CashFlowEvent> events;
    private final BiPredicate<RowType, RowType> equalityChecker;
    private final BiFunction<RowType, RowType, Collection<RowType>> duplicatesMerger;

    public AbstractVtbCashFlowTable(CashFlowEventTable cashFlowEventTable) {
        super(cashFlowEventTable.getReport());
        this.events = unmodifiableCollection(cashFlowEventTable.getData());
        equalityChecker = null;
        duplicatesMerger = null;
    }

    public AbstractVtbCashFlowTable(CashFlowEventTable cashFlowEventTable,
                                    BiPredicate<RowType, RowType> equalityChecker,
                                    BiFunction<RowType, RowType, Collection<RowType>> duplicatesMerger) {
        super(cashFlowEventTable.getReport());
        this.events = unmodifiableCollection(cashFlowEventTable.getData());
        this.equalityChecker = equalityChecker;
        this.duplicatesMerger = duplicatesMerger;
    }

    @Override
    protected Collection<RowType> parseTable() {
        if (equalityChecker == null || duplicatesMerger == null) {
            return events.stream()
                    .flatMap(e -> getRow(e).stream())
                    .collect(Collectors.toList());
        } else {
            return events.stream()
                    .flatMap(e -> getRow(e).stream())
                    .collect(toList(equalityChecker, duplicatesMerger));
        }
    }

    private static <RowType> Collector<RowType, ArrayList<RowType>, ArrayList<RowType>> toList(
            BiPredicate<RowType, RowType> equalityChecker,
            BiFunction<RowType, RowType, Collection<RowType>> duplicatesMerger) {
        return Collector.of(
                ArrayList::new,
                (collection, element) -> addWithEqualityChecker(
                        element,
                        collection,
                        equalityChecker,
                        duplicatesMerger),
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                Collector.Characteristics.IDENTITY_FINISH
        );
    }

    protected abstract Collection<RowType> getRow(CashFlowEventTable.CashFlowEvent event);
}
