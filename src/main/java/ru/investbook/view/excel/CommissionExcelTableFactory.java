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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.EventCashFlow;
import ru.investbook.pojo.Portfolio;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;
import ru.investbook.view.ViewFilter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.investbook.view.excel.CommissionExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class CommissionExcelTableFactory implements TableFactory {
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;

    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        List<EventCashFlow> cashFlows = eventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                        portfolio.getId(),
                        CashFlowType.COMMISSION.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        for (EventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(COMMISSION, Optional.ofNullable(cash.getValue())
                    .map(BigDecimal::negate)
                    .orElse(BigDecimal.ZERO));
            record.put(CURRENCY, cash.getCurrency());
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        return table;
    }
}
