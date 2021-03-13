/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.StockMarketIndexRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.spacious_team.broker.pojo.CashFlowType.CASH;
import static ru.investbook.report.excel.ExcelTableHeader.getColumnsRange;
import static ru.investbook.report.excel.PortfolioAnalysisExcelTableHeader.*;
import static ru.investbook.report.excel.PortfolioStatusExcelTableFactory.minCash;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioAnalysisExcelTableFactory implements TableFactory {
    private static final String ASSETS_GROWTH_FORMULA = getAssetsGrowthFormula();
    private static final String SP500_GROWTH_FORMULA = getSp500GrowthFormula();
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;
    private final StockMarketIndexRepository stockMarketIndexRepository;

    @Override
    public Table create() {
        return createTable(
                getCashFlow(Optional.empty()),
                getCashBalance(Optional.empty()),
                getTotalAssets(Optional.empty()),
                getSp500Index());
    }

    @Override
    public Table create(Portfolio portfolio) {
        return createTable(
                getCashFlow(Optional.of(portfolio)),
                getCashBalance(Optional.of(portfolio)),
                getTotalAssets(Optional.of(portfolio)),
                getSp500Index());
    }

    private Table createTable(List<EventCashFlow> cashFlows,
                              LinkedHashMap<Instant, Map<String, BigDecimal>> cashBalances,
                              LinkedHashMap<Instant, BigDecimal> totalAssets,
                              Map<LocalDate, BigDecimal> sp500) {
        Table table = new Table();
        addInvestmentColumns(cashFlows, table);
        addCashBalanceColumns(cashBalances, table);
        addAssetsColumns(totalAssets, table);

        table.sort(comparing(record -> ((LocalDate) record.get(DATE))));

        addAssetsGrowthColumn(table);
        addSp500GrowthColumn(sp500, table);

        addCurrencyExchangeRateColumns(table);
        return table;
    }

    private void addInvestmentColumns(List<EventCashFlow> cashFlows, Table table) {
        for (EventCashFlow cashFlow : cashFlows) {
            Table.Record record = recordOf(table, cashFlow.getTimestamp(), cashFlow.getCurrency());
            record.merge(INVESTMENT_AMOUNT, cashFlow.getValue(), (v1, v2) -> ((BigDecimal) v1).add(((BigDecimal) v2)));
            record.putIfAbsent(INVESTMENT_AMOUNT_USD, foreignExchangeRateTableFactory
                    .cashConvertToUsdExcelFormula(cashFlow.getCurrency(), INVESTMENT_AMOUNT, EXCHANGE_RATE));
            record.putIfAbsent(TOTAL_INVESTMENT_USD,
                    "=SUM(" + INVESTMENT_AMOUNT_USD.getColumnIndex() + "3:" + INVESTMENT_AMOUNT_USD.getCellAddr() + ")");
        }
    }

    private void addCashBalanceColumns(LinkedHashMap<Instant, Map<String, BigDecimal>> cashBalances, Table table) {
        for (var entry : cashBalances.entrySet()) {
            Instant instant = entry.getKey();
            Table.Record record = recordOf(table, instant);
            Map<String, BigDecimal> currencyValue = entry.getValue();
            record.put(CASH_RUB, currencyValue.get("RUB"));
            record.put(CASH_USD, currencyValue.get("USD"));
            record.put(CASH_EUR, currencyValue.get("EUR"));
            record.put(CASH_CHF, currencyValue.get("CHF"));
            record.put(CASH_GBP, currencyValue.get("GBP"));
            record.put(TOTAL_CASH_USD,
                    foreignExchangeRateTableFactory.cashConvertToUsdExcelFormula("RUB", CASH_RUB, EXCHANGE_RATE) + "+" +
                            CASH_USD.getCellAddr() + "+" +
                            foreignExchangeRateTableFactory.cashConvertToUsdExcelFormula("EUR", CASH_EUR, EXCHANGE_RATE).substring(1) + "+" +
                            foreignExchangeRateTableFactory.cashConvertToUsdExcelFormula("CHF", CASH_CHF, EXCHANGE_RATE).substring(1) + "+" +
                            foreignExchangeRateTableFactory.cashConvertToUsdExcelFormula("GBP", CASH_GBP, EXCHANGE_RATE).substring(1));
        }
    }

    private void addAssetsColumns(LinkedHashMap<Instant, BigDecimal> totalAssets, Table table) {
        for (var entry : totalAssets.entrySet()) {
            Instant instant = entry.getKey();
            BigDecimal assets = entry.getValue();
            Table.Record record = recordOf(table, instant);
            record.put(ASSETS_RUB, assets);
            record.put(ASSETS_USD, foreignExchangeRateTableFactory
                    .cashConvertToUsdExcelFormula("RUB", ASSETS_RUB, EXCHANGE_RATE));

        }
    }

    private static void addAssetsGrowthColumn(Table table) {
        boolean isTotalInvestmentUsdKnown = false;
        for (var record : table) {
            if (!isTotalInvestmentUsdKnown && record.containsKey(TOTAL_INVESTMENT_USD)) {
                isTotalInvestmentUsdKnown = true;
            }
            if (isTotalInvestmentUsdKnown) {
                record.putIfAbsent(TOTAL_INVESTMENT_USD, "=INDIRECT(\"" + TOTAL_INVESTMENT_USD.getColumnIndex() + "\" & ROW() - 1)");
                BigDecimal assetsRub = (BigDecimal) record.get(ASSETS_RUB);
                if (assetsRub != null && assetsRub.compareTo(minCash) > 0) {
                    record.put(ASSETS_GROWTH, ASSETS_GROWTH_FORMULA);
                }
            }
        }
    }

    private static void addSp500GrowthColumn(Map<LocalDate, BigDecimal> sp500, Table table) {
        boolean isSp500ValueKnown = false;
        for (var record : table) {
            LocalDate date = (LocalDate) record.get(DATE);
            BigDecimal value = sp500.get(date);
            if (value != null) {
                isSp500ValueKnown = true;
                record.put(SP500, value);
                record.put(SP500_GROWTH, SP500_GROWTH_FORMULA);
            } else if (isSp500ValueKnown) {
                record.put(SP500, "=INDIRECT(\"" + SP500.getColumnIndex() + "\" & ROW() - 1)");
                record.put(SP500_GROWTH, SP500_GROWTH_FORMULA);
            }
        }
    }

    private void addCurrencyExchangeRateColumns(Table table) {
        if (!table.isEmpty()) {
            foreignExchangeRateTableFactory.appendExchangeRates(table, CURRENCY_NAME, EXCHANGE_RATE);
        }
    }

    private static Table.Record recordOf(Table table, Instant instant, String investmentCurrency) {
        LocalDate date = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        return table.stream()
                .filter(record -> date.equals(record.get(DATE)) && investmentCurrency.equals(record.get(INVESTMENT_CURRENCY)))
                .findAny()
                .orElseGet(() -> {
                    Table.Record record = table.addNewRecord();
                    record.put(DATE, date);
                    record.put(INVESTMENT_CURRENCY, investmentCurrency);
                    return record;
                });
    }

    private static Table.Record recordOf(Table table, Instant instant) {
        LocalDate date = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        return table.stream()
                .filter(record -> date.equals(record.get(DATE)))
                .findAny()
                .orElseGet(() -> {
                    Table.Record record = table.addNewRecord();
                    record.put(DATE, date);
                    return record;
                });
    }

    private List<EventCashFlow> getCashFlow(Optional<Portfolio> portfolio) {
        ViewFilter viewFilter = ViewFilter.get();
        return portfolio
                .map(value ->
                        eventCashFlowRepository
                                .findByPortfolioIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                                        value.getId(),
                                        CASH.getId(),
                                        viewFilter.getFromDate(),
                                        viewFilter.getToDate()))
                .orElseGet(() ->
                        eventCashFlowRepository
                                .findByCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                                        CASH.getId(),
                                        viewFilter.getFromDate(),
                                        viewFilter.getToDate()))
                .stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns cash balance information for all portfolios.
     *
     * @return map of date -> currency -> value
     */
    private LinkedHashMap<Instant, Map<String, BigDecimal>> getCashBalance(Optional<Portfolio> portfolio) {
        List<PortfolioProperty> portfolioCashes = getPortfolioProperty(portfolio, PortfolioPropertyType.CASH);
        return getAllPortfolioCashBalance(portfolioCashes);
    }

    // TODO add second method with PortfolioPropertyType.TOTAL_ASSETS_USD and combine both results late
    private LinkedHashMap<Instant, BigDecimal> getTotalAssets(Optional<Portfolio> portfolio) {
        List<PortfolioProperty> assets = getPortfolioProperty(portfolio, PortfolioPropertyType.TOTAL_ASSETS_RUB);
        return getAllPortfolioTotalAssets(assets);
    }

    private List<PortfolioProperty> getPortfolioProperty(Optional<Portfolio> portfolio, PortfolioPropertyType property) {
        ViewFilter viewFilter = ViewFilter.get();
        List<PortfolioProperty> properties = portfolio
                .map(value ->
                        portfolioPropertyRepository
                                .findByPortfolioIdAndPropertyAndTimestampBetweenOrderByTimestampDesc(
                                        value.getId(),
                                        property.name(),
                                        viewFilter.getFromDate(),
                                        viewFilter.getToDate()))
                .orElseGet(() ->
                        portfolioPropertyRepository
                                .findByPropertyAndTimestampBetweenOrderByTimestampDesc(
                                        property.name(),
                                        viewFilter.getFromDate(),
                                        viewFilter.getToDate()))
                .stream()
                .map(portfolioPropertyConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(properties);
        return properties;
    }

    private Map<LocalDate, BigDecimal> getSp500Index() {
        ViewFilter viewFilter = ViewFilter.get();
        return stockMarketIndexRepository
                .getByDateBetweenOrderByDate(
                        LocalDate.ofInstant(viewFilter.getFromDate(), ZoneId.systemDefault()),
                        LocalDate.ofInstant(viewFilter.getToDate(), ZoneId.systemDefault()))
                .stream()
                .collect(Collectors.toMap(StockMarketIndexEntity::getDate, StockMarketIndexEntity::getSp500));
    }

    /**
     * Sums cash with same currency for all portfolios and markets, groups result by date.
     *
     * @return map of date -> currency -> value
     */
    private LinkedHashMap<Instant, Map<String, BigDecimal>> getAllPortfolioCashBalance(List<PortfolioProperty> portfolioCashes) {
        int portfolioCount = (int) portfolioCashes.stream()
                .map(PortfolioProperty::getPortfolio)
                .distinct()
                .count();
        // summing cash values with same currency
        List<PortfolioInstantCurrencyValue> balances = portfolioCashes.stream()
                .map(portfolioProperty -> {
                    Map<String, BigDecimal> currencyValue = PortfolioCash.valueOf(portfolioProperty.getValue())
                            .stream()
                            .collect(groupingBy(PortfolioCash::getCurrency,
                                    reducing(BigDecimal.ZERO, PortfolioCash::getValue, BigDecimal::add)));
                    return PortfolioInstantCurrencyValue.builder()
                            .portfolio(portfolioProperty.getPortfolio())
                            .instant(portfolioProperty.getTimestamp())
                            .currencyValue(currencyValue)
                            .build();
                })
                .collect(Collectors.toList());
        // portfolio -> summed balances
        Map<String, PortfolioInstantCurrencyValue> lastBalances = new HashMap<>();
        // date -> currency -> cash balance
        LinkedHashMap<Instant, Map<String, BigDecimal>> allPortfolioCashBalance = new LinkedHashMap<>();
        for (PortfolioInstantCurrencyValue balance : balances) {
            lastBalances.put(balance.getPortfolio(), balance);
            if (lastBalances.size() >= portfolioCount) {
                Map<String, BigDecimal> joinedBalance = lastBalances.values()
                        .stream()
                        .map(PortfolioInstantCurrencyValue::getCurrencyValue)
                        .reduce(new HashMap<>(), (currencyValue1, currencyValue2) -> {
                            currencyValue2.forEach((currency, value) -> currencyValue1.merge(currency, value, BigDecimal::add));
                            return currencyValue1;
                        });
                allPortfolioCashBalance.put(balance.getInstant(), joinedBalance);
            }
        }
        return allPortfolioCashBalance;
    }

    @Getter
    @Builder
    private static class PortfolioInstantCurrencyValue {
        private final String portfolio;
        private final Instant instant;
        private final Map<String, BigDecimal> currencyValue;
    }

    private LinkedHashMap<Instant, BigDecimal> getAllPortfolioTotalAssets(List<PortfolioProperty> assets) {
        int portfolioCount = (int) assets.stream()
                .map(PortfolioProperty::getPortfolio)
                .distinct()
                .count();
        // portfolio -> value
        Map<String, BigDecimal> lastTotalAssets = new HashMap<>();
        // date-time -> summed values
        LinkedHashMap<Instant, BigDecimal> allPortfolioSummedValues = new LinkedHashMap<>();
        for (PortfolioProperty property : assets) {
            lastTotalAssets.put(property.getPortfolio(), BigDecimal.valueOf(parseDouble(property.getValue())));
            if (lastTotalAssets.size() >= portfolioCount) {
                BigDecimal sum = lastTotalAssets.values()
                        .stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                allPortfolioSummedValues.put(property.getTimestamp(), sum);
            }
        }
        return allPortfolioSummedValues;
    }

    /**
     * (CurrentAssets / CurrentInvestment) * (InitialInvestment / InitialAssets) - 1,
     * where
     * CurrentInvestment = (InitialInvestment + InvestmentDelta),
     * InitialInvestment = InitialAssets
     */
    private static String getAssetsGrowthFormula() {
        String initialInvestment = getInitialAssetsUsdFormula();
        String investmentDelta = "(" + TOTAL_INVESTMENT_USD.getCellAddr() + "-" + getFirstKnownInvestmentUsdFormula() + ")";
        String currentInvestment = "(" + initialInvestment + "+" + investmentDelta + ")";
        String assetsGrowth = ASSETS_USD.getCellAddr() + "*100/" + currentInvestment + "-100";
        return "=IF(" + TOTAL_INVESTMENT_USD.getCellAddr() + ">0," + assetsGrowth + ",\"\")";
    }

    private static String getFirstKnownInvestmentUsdFormula() {
        String firstNonEmptyRow = getInitialInvestmentUsdAndInitialAssetsUsdRowFormula();
        return "INDEX(" +
                getColumnsRange(TOTAL_INVESTMENT_USD, 3, ASSETS_USD, 10000) + "," +
                firstNonEmptyRow + ",1)";
    }

    private static String getInitialAssetsUsdFormula() {
        String firstNonEmptyRow = getInitialInvestmentUsdAndInitialAssetsUsdRowFormula();
        return "INDEX(" +
                getColumnsRange(TOTAL_INVESTMENT_USD, 3, ASSETS_USD, 10000) + "," +
                firstNonEmptyRow + "," + (1 + Math.abs(ASSETS_USD.ordinal() - TOTAL_INVESTMENT_USD.ordinal())) + ")";
    }

    private static String getSp500GrowthFormula() {
        String initialSp500Value = getSp500InitialValue();
        String nonEmptyValues = "AND(" + SP500.getCellAddr() + "<>\"\"," + initialSp500Value + "<>\"\")";
        String growth = SP500.getCellAddr() + "*100/" + initialSp500Value + "-100";
        return "=IF(" + nonEmptyValues + "," + growth + ",\"\")";
    }
    private static String getSp500InitialValue() {
        String firstNonEmptyRow = getInitialInvestmentUsdAndInitialAssetsUsdRowFormula();
        return "INDEX(" +
                getColumnsRange(SP500, 3, SP500, 10000) + "," +
                firstNonEmptyRow + ",1)";
    }

    private static String getInitialInvestmentUsdAndInitialAssetsUsdRowFormula() {
        String firstNonEmptyTotalInvestmentUsdRow = "MATCH(true,INDEX((" + TOTAL_INVESTMENT_USD.getRange(3, 10000) + "<>0),0),0)";
        String firstNonEmptyAssetsUsdRow = "MATCH(true,INDEX((" + ASSETS_USD.getRange(3, 10000) + "<>0),0),0)";
        return "MAX(" + firstNonEmptyTotalInvestmentUsdRow + "," + firstNonEmptyAssetsUsdRow + ")";
    }
}
