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

package ru.portfolio.portfolio.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.EventCashFlowConverter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.repository.EventCashFlowRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.CashFlowExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class CashFlowExcelTableFactory implements TableFactory {
    // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
    private static final String DAYS_COUNT_FORMULA = "=DAYS360(" + DATE.getCellAddr() + ",TODAY())";
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;

    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        List<EventCashFlow> cashFlows = eventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdOrderByTimestamp(
                        portfolio.getId(),
                        CashFlowType.CASH.getId())
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        for (EventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(CASH, cash.getValue());
            record.put(CURRENCY, cash.getCurrency());
            record.put(DAYS_COUNT, DAYS_COUNT_FORMULA);
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        return table;
    }
}
