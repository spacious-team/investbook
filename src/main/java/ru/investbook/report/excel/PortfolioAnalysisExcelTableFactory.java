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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.springframework.stereotype.Component;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.converter.PortfolioCashConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.PortfolioCashEntity;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.Table;
import ru.investbook.report.TableFactory;
import ru.investbook.report.ViewFilter;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioCashRepository;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.StockMarketIndexRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Double.isFinite;
import static java.lang.Double.parseDouble;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;
import static org.spacious_team.broker.pojo.CashFlowType.CASH;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_RUB;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_USD;
import static ru.investbook.report.ForeignExchangeRateService.RUB;
import static ru.investbook.report.excel.ExcelTableHeader.getColumnsRange;
import static ru.investbook.report.excel.PortfolioAnalysisExcelTableHeader.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfolioAnalysisExcelTableFactory implements TableFactory {
    private static final String SP500_GROWTH_FORMULA = getSp500GrowthFormula();
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PortfolioCashRepository portfolioCashRepository;
    private final PortfolioCashConverter portfolioCashConverter;
    private final ForeignExchangeRateTableFactory foreignExchangeRateTableFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final StockMarketIndexRepository stockMarketIndexRepository;
    private final TreeMap<Instant, BigDecimal> emptyTreeMap = new TreeMap<>();
    private final Set<String> totalAssetsProperty = Set.of(
            TOTAL_ASSETS_RUB.name(),
            TOTAL_ASSETS_USD.name());

    @Override
    public Table create(Collection<String> portfolios) {
        List<EventCashFlow> cashFlow = getCashFlow(portfolios);
        return createTable(
                cashFlow,
                getCashBalance(portfolios),
                getTotalAssets(portfolios, cashFlow),
                getSp500Index());
    }

    @Override
    public Table create(Portfolio portfolio) {
        List<EventCashFlow> cashFlow = getCashFlow(singleton(portfolio.getId()));
        return createTable(
                cashFlow,
                getCashBalance(singleton(portfolio.getId())),
                getTotalAssets(singleton(portfolio.getId()), cashFlow),
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

        fillEmptyTotalInvestmentCells(table);

        addCurrencyExchangeRateColumns(table);
        return table;
    }

    private void addInvestmentColumns(List<EventCashFlow> cashFlows, Table table) {
        for (EventCashFlow cashFlow : cashFlows) {
            Table.Record record = recordOf(table, cashFlow.getTimestamp(), cashFlow.getCurrency());
            record.merge(INVESTMENT_AMOUNT, cashFlow.getValue(), (v1, v2) -> ((BigDecimal) v1).add(((BigDecimal) v2)));
            record.computeIfAbsent(INVESTMENT_AMOUNT_USD, $ -> foreignExchangeRateTableFactory
                    .cashConvertToUsdExcelFormula(cashFlow.getCurrency(), INVESTMENT_AMOUNT, EXCHANGE_RATE));
            record.computeIfAbsent(TOTAL_INVESTMENT_USD, $ ->
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

    private void addAssetsGrowthColumn(Table table) {
        try {
            double usdToRubExchangeRate = foreignExchangeRateService.getExchangeRateToRub("USD").doubleValue();
            Double divider = null;
            double investmentUsd = 0;
            Double prevAssetsGrownValue = null;
            for (var record : table) {
                investmentUsd += getInvestmentUsd(record, usdToRubExchangeRate);
                Number assetsRub = (Number) record.get(ASSETS_RUB);
                if (assetsRub != null) {
                    double assetsUsd = assetsRub.doubleValue() / usdToRubExchangeRate;
                    divider = updateDivider(divider, assetsUsd, investmentUsd, prevAssetsGrownValue);
                    investmentUsd = 0;
                    if (divider != null) {
                        record.put(ASSETS_GROWTH, "=(" + ASSETS_USD.getCellAddr() + "/" + divider + "-1)*100");
                        prevAssetsGrownValue = assetsUsd / divider;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Ошибка при расчете роста активов в %", e);
        }
    }

    private double getInvestmentUsd(Table.Record record, double usdToRubExchangeRate) {
        BigDecimal investment = (BigDecimal) record.get(INVESTMENT_AMOUNT);
        if (investment != null) {
            String fromCurrency = record.get(INVESTMENT_CURRENCY).toString();
            if (Objects.equals(fromCurrency, "RUB")) {
                // используем тот же коэффициент-курс для приведения, что и в вызывающем цикле
                return investment.doubleValue() / usdToRubExchangeRate;
            } else {
                // придется обращаться к сервису и зависеть от его реализации
                return foreignExchangeRateService.convertValueToCurrency(investment, fromCurrency, "USD")
                        .doubleValue();
            }
        }
        return 0;
    }

    private Double updateDivider(Double divider, double assetsUsd, double investmentUsd, Double prevAssetsGrowth) {
        if (divider == null) {
            if (prevAssetsGrowth == null) {
                // начинаем график роста активов с нулевой отметки
                divider = assetsUsd;
            } else {
                // счет был опустошен, сейчас деньги снова заведены,
                // график роста активов остановился на отметке prevAssetsGrowth, начинаем с него же
                divider = assetsUsd / prevAssetsGrowth;
            }
        } else {
            double assetsBeforeInvestment = assetsUsd - investmentUsd;
            divider = divider * assetsUsd / assetsBeforeInvestment;
        }
        divider = (!isFinite(divider) || divider < 0.0001) ? null : divider;
        return divider;
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
                record.put(SP500, "=" + SP500.getRelativeCellAddr(-1, 0));
                record.put(SP500_GROWTH, SP500_GROWTH_FORMULA);
            }
        }
    }

    private void fillEmptyTotalInvestmentCells(Table table) {
        boolean isTotalInvestmentUsdKnown = false;
        for (var record : table) {
            isTotalInvestmentUsdKnown = isTotalInvestmentUsdKnown || record.containsKey(TOTAL_INVESTMENT_USD);
            if (isTotalInvestmentUsdKnown) {
                record.computeIfAbsent(TOTAL_INVESTMENT_USD,
                        $ -> "=" + TOTAL_INVESTMENT_USD.getRelativeCellAddr(-1, 0));
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

    private List<EventCashFlow> getCashFlow(Collection<String> portfolios) {
        ViewFilter viewFilter = ViewFilter.get();
        List<EventCashFlowEntity> entities = portfolios.isEmpty() ?
                eventCashFlowRepository
                        .findByCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                                CASH.getId(),
                                viewFilter.getFromDate(),
                                viewFilter.getToDate()) :
                eventCashFlowRepository
                        .findByPortfolioIdInAndCashFlowTypeIdAndTimestampBetweenOrderByTimestamp(
                                portfolios,
                                CASH.getId(),
                                viewFilter.getFromDate(),
                                viewFilter.getToDate());
        return entities.stream()
                .map(eventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns cash balance information for all portfolios
     *
     * @return map of date -> currency -> value
     */
    private LinkedHashMap<Instant, Map<String, BigDecimal>> getCashBalance(Collection<String> portfolios) {
        List<PortfolioCashEntity> portfolioCashEntities = portfolios.isEmpty() ?
                portfolioCashRepository.findAll() :
                portfolioCashRepository.findByPortfolioIn(portfolios);
        List<PortfolioCash> portfolioCashes = portfolioCashEntities.stream()
                .map(portfolioCashConverter::fromEntity)
                .toList();
        List<PortfolioInstantCurrencyValue> balances = sumCashWithSameCurrency(portfolioCashes);
        int portfolioCount = countPortfolios(portfolioCashes);
        return getAllPortfolioCashBalance(balances, portfolioCount);
    }

    private LinkedHashMap<Instant, BigDecimal> getTotalAssets(Collection<String> portfolios,
                                                              List<EventCashFlow> cashFlows) {
        List<PortfolioProperty> assets = getPortfolioProperty(portfolios, totalAssetsProperty);
        return getAllPortfolioTotalAssets(assets, cashFlows);
    }

    private List<PortfolioProperty> getPortfolioProperty(Collection<String> portfolios, Collection<String> propertyTypes) {
        ViewFilter viewFilter = ViewFilter.get();
        List<PortfolioPropertyEntity> entities = portfolios.isEmpty() ?
                portfolioPropertyRepository
                        .findByPropertyInAndTimestampBetweenOrderByTimestampAsc(
                                propertyTypes,
                                viewFilter.getFromDate(),
                                viewFilter.getToDate()) :
                portfolioPropertyRepository
                        .findByPortfolioIdInAndPropertyInAndTimestampBetweenOrderByTimestampAsc(
                                portfolios,
                                propertyTypes,
                                viewFilter.getFromDate(),
                                viewFilter.getToDate());
        return entities.stream()
                .map(portfolioPropertyConverter::fromEntity)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<LocalDate, BigDecimal> getSp500Index() {
        ViewFilter viewFilter = ViewFilter.get();
        return stockMarketIndexRepository
                .findByDateBetweenOrderByDate(
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
    private static LinkedHashMap<Instant, Map<String, BigDecimal>> getAllPortfolioCashBalance(
            List<PortfolioInstantCurrencyValue> balances, int portfolioCount) {
        // date -> currency -> cash balance
        LinkedHashMap<Instant, Map<String, BigDecimal>> allPortfolioCashBalance = new LinkedHashMap<>();
        // temp var: portfolio -> summed balances
        Map<String, PortfolioInstantCurrencyValue> lastBalances = new HashMap<>();
        for (PortfolioInstantCurrencyValue balance : balances) {
            lastBalances.put(balance.portfolio(), balance);
            if (lastBalances.size() >= portfolioCount) {
                Map<String, BigDecimal> joinedBalance = lastBalances.values()
                        .stream()
                        .map(PortfolioInstantCurrencyValue::currencyValueMap)
                        .reduce(new HashMap<>(), (currencyValue1, currencyValue2) -> {
                            currencyValue2.forEach((currency, value) -> currencyValue1.merge(currency, value, BigDecimal::add));
                            return currencyValue1;
                        });
                allPortfolioCashBalance.put(balance.instant(), joinedBalance);
            }
        }
        return allPortfolioCashBalance;
    }

    private static int countPortfolios(List<PortfolioCash> portfolioCashes) {
        return (int) portfolioCashes.stream()
                .map(PortfolioCash::getPortfolio)
                .distinct()
                .count();
    }

    private static List<PortfolioInstantCurrencyValue> sumCashWithSameCurrency(List<PortfolioCash> portfolioCashes) {
        var portfolioTimestampCurrencyMap = portfolioCashes.stream()
                .collect(
                        groupingBy(PortfolioCash::getPortfolio,
                                groupingBy(PortfolioCash::getTimestamp,
                                        groupingBy(PortfolioCash::getCurrency,
                                                reducing(BigDecimal.ZERO, PortfolioCash::getValue, BigDecimal::add)))));
        List<PortfolioInstantCurrencyValue> result = new ArrayList<>(portfolioCashes.size());
        portfolioTimestampCurrencyMap.forEach((portfolio, timestampCurrencyValueMap) ->
                timestampCurrencyValueMap.forEach((timestamp, currencyValueMap) ->
                        result.add(new PortfolioInstantCurrencyValue(portfolio, timestamp, currencyValueMap))
                ));
        result.sort(Comparator.comparing(PortfolioInstantCurrencyValue::instant));
        return result;
    }

    private record PortfolioInstantCurrencyValue(String portfolio,
                                                 Instant instant,
                                                 Map<String, BigDecimal> currencyValueMap) {
    }

    /**
     * Assets in ruble
     */
    private LinkedHashMap<Instant, BigDecimal> getAllPortfolioTotalAssets(List<PortfolioProperty> assets,
                                                                          List<EventCashFlow> cashFlows) {
        List<PortfolioAssetsInRub> summedAssetsInRub = sumValuesOfSameInstantInRub(assets);
        // temp var: portfolio -> assets
        Map<String, BigDecimal> lastTotalAssets = initPortfoliosByZero(assets);
        Instant lastInstant = Instant.MIN;
        // date-time -> summed assets for all portfolios
        LinkedHashMap<Instant, BigDecimal> allPortfolioSummedAssets = new LinkedHashMap<>();

        // portfolio -> timestamp -> cash flows in rub
        Map<String, TreeMap<Instant, BigDecimal>> rubCashFlowsGroupedByPortfolio =
                convertCashFlowsToRubAndGroupByPortfolio(cashFlows);

        for (PortfolioAssetsInRub updatingAssets : summedAssetsInRub) {
            updateKnownPortfolioAssets(lastTotalAssets, updatingAssets, rubCashFlowsGroupedByPortfolio, lastInstant);
            lastInstant = updatingAssets.instant();
            BigDecimal sum = lastTotalAssets.values()
                    .stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            allPortfolioSummedAssets.put(updatingAssets.instant(), sum);
        }
        return allPortfolioSummedAssets;
    }

    /**
     * Sums PortfolioPropertyType.TOTAL_ASSETS_RUB and TOTAL_ASSETS_USD if both exists for same timestamp
     */
    private List<PortfolioAssetsInRub> sumValuesOfSameInstantInRub(List<PortfolioProperty> assets) {
        // portfolio -> Instant -> value in RUB
        Map<String, Map<Instant, BigDecimal>> portfolioInstantValueInRub = assets.stream()
                .collect(groupingBy(PortfolioProperty::getPortfolio,
                        toMap(PortfolioProperty::getTimestamp, this::convertAssetsToRub, BigDecimal::add)));
        List<PortfolioAssetsInRub> summedAssetsInRub = new ArrayList<>();
        portfolioInstantValueInRub.forEach((portfolio, instantValueInRub) ->
                instantValueInRub.forEach((instant, valueInRub) ->
                        summedAssetsInRub.add(new PortfolioAssetsInRub(portfolio, instant, valueInRub))));
        summedAssetsInRub.sort(comparing(PortfolioAssetsInRub::instant));
        return summedAssetsInRub;
    }

    private record PortfolioAssetsInRub(String portfolio, Instant instant, BigDecimal valueInRub) {
    }

    private BigDecimal convertAssetsToRub(PortfolioProperty updatingAssets) {
        String currency = switch (updatingAssets.getProperty()) {
            case TOTAL_ASSETS_RUB -> RUB;
            case TOTAL_ASSETS_USD -> "USD";
        };
        return foreignExchangeRateService.convertValueToCurrency(getAssets(updatingAssets), currency, RUB);
    }

    private Map<String, BigDecimal> initPortfoliosByZero(Collection<PortfolioProperty> assets) {
        return assets.stream()
                .map(PortfolioProperty::getPortfolio)
                .distinct()
                .collect(toMap(Function.identity(), $ -> BigDecimal.ZERO));
    }

    private Map<String, TreeMap<Instant, BigDecimal>> convertCashFlowsToRubAndGroupByPortfolio(List<EventCashFlow> cashFlows) {
        return cashFlows.stream()
                .collect(groupingBy(EventCashFlow::getPortfolio,
                        toMap(EventCashFlow::getTimestamp,
                                v -> foreignExchangeRateService.convertValueToCurrency(v.getValue(), v.getCurrency(), RUB),
                                BigDecimal::add,
                                TreeMap::new)));
    }

    private void updateKnownPortfolioAssets(Map<String, BigDecimal> lastPortfolioAssets,
                                            PortfolioAssetsInRub updatingAssets,
                                            Map<String, TreeMap<Instant, BigDecimal>> rubCashFlowsGroupedByPortfolio,
                                            Instant lastInstant) {
        String updatingPortfolio = updatingAssets.portfolio();
        lastPortfolioAssets.put(updatingPortfolio, updatingAssets.valueInRub());
        lastPortfolioAssets.replaceAll((portfolio, portfolioAssets) ->
                portfolio.equals(updatingPortfolio) ? portfolioAssets :
                        // update other portfolios by invested sum
                        rubCashFlowsGroupedByPortfolio.getOrDefault(portfolio, emptyTreeMap)
                                .subMap(lastInstant, false, updatingAssets.instant(), true)
                                .values()
                                .stream()
                                .reduce(portfolioAssets, BigDecimal::add));
    }

    private static BigDecimal getAssets(PortfolioProperty property) {
        try {
            return BigDecimal.valueOf(parseDouble(property.getValue()));
        } catch (Exception e) {
            log.error("В поле актив ожидается число: {}", property);
            return BigDecimal.ZERO;
        }
    }

    private static String getSp500GrowthFormula() {
        String initialSp500Value = getSp500InitialValue();
        String nonEmptyValues = "AND(" + SP500.getCellAddr() + "<>\"\"," + initialSp500Value + "<>\"\")";
        String growth = SP500.getCellAddr() + "*100/" + initialSp500Value + "-100";
        return "=IF(" + nonEmptyValues + "," + growth + ",\"\")";
    }

    private static String getSp500InitialValue() {
        String firstNonEmptyRow = "MATCH(true,INDEX((" + ASSETS_USD.getRange(3, 10000) + "<>0),0),0)";
        return "INDEX(" +
                getColumnsRange(SP500, 3, SP500, 10000) + "," +
                firstNonEmptyRow + ",1)";
    }
}
