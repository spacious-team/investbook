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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.EventCashFlowConverter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.Portfolio;
import ru.portfolio.portfolio.pojo.PortfolioPropertyType;
import ru.portfolio.portfolio.repository.EventCashFlowRepository;
import ru.portfolio.portfolio.repository.PortfolioPropertyRepository;
import ru.portfolio.portfolio.view.Table;
import ru.portfolio.portfolio.view.TableFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.CashFlowExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashFlowExcelTableFactory implements TableFactory {
    // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
    private static final String DAYS_COUNT_FORMULA = "=DAYS360(" + DATE.getCellAddr() + ",TODAY())";
    private static final List<String> currencies = Arrays.asList("USD", "EUR", "GBP", "CHF");
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final PortfolioPropertyRepository portfolioPropertyRepository;

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
            record.put(CASH_RUB, cashConvertToRubExcelFormula(cash));
            record.put(DAYS_COUNT, DAYS_COUNT_FORMULA);
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        appendExchangeRates(table);
        return table;
    }

    private static String cashConvertToRubExcelFormula(EventCashFlow cash) {
        int rowNum = currencies.indexOf(cash.getCurrency());
        if (rowNum == -1) {
            return "=" + CASH.getCellAddr();
        } else {
            return "=" + CASH.getCellAddr() + "*" + EXCHANGE_RATE.getColumnIndex() + (rowNum + 3); // 2 rows = header + total
        }
    }

    private void appendExchangeRates(Table table) {
        for (int i = 0; i < currencies.size(); i++) {
            String currency = currencies.get(i);
            Table.Record record = table.get(i);
            record.put(CURRENCY_NAME, currency);
            try {
                PortfolioPropertyType property = PortfolioPropertyType.valueOf(currency + "_EXCHANGE_RATE");
                BigDecimal exchangeRate = portfolioPropertyRepository
                        .findFirstByPropertyOrderByTimestampDesc(property.name())
                        .map(v -> BigDecimal.valueOf(Double.parseDouble(v.getValue())))
                        .orElseThrow(RuntimeException::new);

                record.put(EXCHANGE_RATE, exchangeRate);
            } catch (Exception e) {
                int defaultExchangeRate = 75;
                switch (currency) {
                    case "EUR": defaultExchangeRate = 85; break;
                    case "GBP": defaultExchangeRate = 95; break;
                }
                log.debug("Не могу в БД найти курс валюты {}, использую значение по умолчанию = {}",
                        currency, defaultExchangeRate);
                record.put(EXCHANGE_RATE, defaultExchangeRate);
            }
        }
    }
}
