/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioPropertyRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.investbook.report.excel.CashFlowExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CashFlowExcelTableFactory implements TableFactory {
    // TODO DAYS() excel function not impl by Apache POI: https://bz.apache.org/bugzilla/show_bug.cgi?id=58468
    private static final String DAYS_COUNT_FORMULA = "=DAYS360(" + DATE.getCellAddr() + ",TODAY())";
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;
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
            record.put(CASH_RUB, foreignExchangeRateTableFactory.cashConvertToRubExcelFormula(cash.getCurrency(), CASH, EXCHANGE_RATE));
            record.put(DAYS_COUNT, DAYS_COUNT_FORMULA);
            record.put(DESCRIPTION, cash.getDescription());
            table.add(record);
        }
        if (!cashFlows.isEmpty()) {
            addLiquidationValueRow(table);
        }
        appendCurrencyInfo(portfolio, table);
        return table;
    }

    private void addLiquidationValueRow(Table table) {
        Table.Record record = new Table.Record();
        record.put(DATE, Instant.now());
        record.put(CASH, "=-" + LIQUIDATION_VALUE_RUB.getColumnIndex() + "2");
        record.put(CURRENCY, "RUB");
        record.put(CASH_RUB, "=" + CASH.getCellAddr());
        record.put(DESCRIPTION, "Ликвидная стоимость активов, доступная к выводу");
        table.add(record);
    }

    private void appendCurrencyInfo(Portfolio portfolio, Table table) {
        foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
        appendCashBalance(portfolio, table);
    }

    public void appendCashBalance(Portfolio portfolio, Table table) {
        Map<String, BigDecimal> currencyToValues = getCashBalances(portfolio);
        Table.Record rubCashBalanceRecord = null;
        for (Table.Record record : table) {
            String currency = Optional.ofNullable((String) record.get(CURRENCY_NAME))
                    .map(String::toUpperCase)
                    .orElse(null);
            if (currency != null) {
                BigDecimal value = currencyToValues.get(currency);
                record.put(CASH_BALANCE, value);
            } else {
                rubCashBalanceRecord = record;
                break;
            }
        }
        // print RUB after all already printed currencies
        BigDecimal rubCash = currencyToValues.get("RUB");
        if (rubCash != null) {
            if (rubCashBalanceRecord == null) {
                rubCashBalanceRecord = new Table.Record();
                table.add(rubCashBalanceRecord);
            }
            rubCashBalanceRecord.put(CASH_BALANCE, rubCash);
            rubCashBalanceRecord.put(CURRENCY_NAME, "RUB");
            rubCashBalanceRecord.put(EXCHANGE_RATE, 1);
        }
    }

    private Map<String, BigDecimal> getCashBalances(Portfolio portfolio) {
        try {
            Instant atTime = Instant.ofEpochSecond(Math.min(
                    ViewFilter.get().getToDate().getEpochSecond(),
                    Instant.now().getEpochSecond()));
            return getPortfolioCash(portfolio, atTime)
                    .map(PortfolioCash::deserialize)
                    .orElse(Collections.emptyList())
                    .stream()
                    .collect(Collectors.toMap(c -> c.getCurrency().toUpperCase(), PortfolioCash::getValue, BigDecimal::add));
        } catch (Exception e) {
            log.warn("Ошибка при десериализации свойства CASH", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Возвращает последний известный остаток денежных средств соответствующей дате, не позже указанной.
     */
    private Optional<String> getPortfolioCash(Portfolio portfolio, Instant atInstant) {
        return portfolioPropertyRepository
                .findFirstByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(
                        portfolio.getId(),
                        PortfolioPropertyType.CASH.name(),
                        Instant.ofEpochSecond(0),
                        atInstant)
                .map(PortfolioPropertyEntity::getValue);
    }
}
