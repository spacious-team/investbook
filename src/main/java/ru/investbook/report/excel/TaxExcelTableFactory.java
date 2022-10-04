/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.report.excel;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.EventCashFlowRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static ru.investbook.report.excel.TaxExcelTableHeader.*;

@Component
@RequiredArgsConstructor
public class TaxExcelTableFactory implements TableFactory {
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;

    @Override
    public Table create(Portfolio portfolio) {
        Table table = new Table();
        List<EventCashFlow> cashFlows = eventCashFlowRepository
                .findByPortfolioIdInAndCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                        singleton(portfolio.getId()),
                        CashFlowType.TAX.getId(),
                        ViewFilter.get().getFromDate(),
                        ViewFilter.get().getToDate())
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));

        cashFlows.stream()
                .filter(cash -> !isDividendOrCouponTax(cash.getDescription()))
                .forEach(cash -> addRecordToTable(table, cash));

        if (!cashFlows.isEmpty()) {
            foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
        }

        return table;
    }

    private void addRecordToTable(Table table, EventCashFlow cash) {
        Table.Record record = new Table.Record();
        record.put(DATE, cash.getTimestamp());
        record.put(TAX, Optional.ofNullable(cash.getValue())
                .map(BigDecimal::negate)
                .orElse(BigDecimal.ZERO));
        record.put(CURRENCY, cash.getCurrency());
        record.put(TAX_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(),
                TAX, EXCHANGE_RATE));
        record.put(DESCRIPTION, cash.getDescription());
        table.add(record);
    }

    static boolean isDividendOrCouponTax(String description) {
        if (description == null) {
            return false;
        }
        String lowercasedDescription = description.toLowerCase();
        return lowercasedDescription.contains("дивиденд") || lowercasedDescription.contains("купон");
    }
}
