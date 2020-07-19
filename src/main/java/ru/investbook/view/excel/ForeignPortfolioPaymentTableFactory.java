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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.EventCashFlow;
import ru.investbook.pojo.Portfolio;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.view.Table;
import ru.investbook.view.TableFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.investbook.pojo.CashFlowType.*;
import static ru.investbook.view.excel.ForeignPortfolioPaymentTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForeignPortfolioPaymentTableFactory implements TableFactory {
    /** tax accounted by {@link TaxExcelTableFactory} */
    private static final CashFlowType[] PAY_TYPES = new CashFlowType[]{AMORTIZATION, REDEMPTION, COUPON, DIVIDEND};
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;

    @Override
    public Table create(Portfolio portfolio) {
        List<EventCashFlow> cashFlows = getCashFlows(portfolio);
        return getTable(cashFlows);
    }

    private ArrayList<EventCashFlow> getCashFlows(Portfolio portfolio) {
        return eventCashFlowRepository
                .findByPortfolioIdAndCashFlowTypeIdInOrderByTimestampDesc(
                        portfolio.getId(),
                        Stream.of(PAY_TYPES)
                                .map(CashFlowType::getId)
                                .collect(Collectors.toList()))
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Table getTable(List<EventCashFlow> cashFlows) {
        Table table = new Table();
        for (EventCashFlow cash : cashFlows) {
            Table.Record record = new Table.Record();
            record.put(DATE, cash.getTimestamp());
            record.put(ForeignPortfolioPaymentTableHeader.CASH, cash.getValue());
            record.put(CURRENCY, cash.getCurrency());
            record.put(CASH_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(),
                    ForeignPortfolioPaymentTableHeader.CASH, EXCHANGE_RATE));
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        if (!cashFlows.isEmpty()) {
            foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
        }
        return table;
    }
}
