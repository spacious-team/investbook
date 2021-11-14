/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Service;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.ControllerHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static java.util.stream.Collectors.*;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetsAndCashService {
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final PortfolioRepository portfolioRepository;

    public Set<String> getActivePortfolios() {
        return ControllerHelper.getActivePortfolios(portfolioRepository);
    }

    public Optional<BigDecimal> getAssets(Collection<String> portfolios) {
        Collection<BigDecimal> portfolioAssets = portfolios.stream()
                .map(this::getTotalAssetsInRub)
                .collect(toList());
        if (portfolioAssets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(portfolioAssets.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));

    }

    public BigDecimal getTotalAssetsInRub(String portfolio) {
        Collection<PortfolioPropertyEntity> assets = new ArrayList<>(2);
        getTotalAssets(portfolio, PortfolioPropertyType.TOTAL_ASSETS_RUB).ifPresent(assets::add);
        getTotalAssets(portfolio, PortfolioPropertyType.TOTAL_ASSETS_USD).ifPresent(assets::add);
        // finds last day assets
        Map<String, BigDecimal> values = assets.stream()
                .collect(groupingBy(PortfolioPropertyEntity::getTimestamp, TreeMap::new, toList())) // groups by date
                .lastEntry() // finds last date assets
                .getValue()
                .stream()
                .collect(toMap(AssetsAndCashService::getCurrency, AssetsAndCashService::parseTotalAssetsIfCan));
        return convertToRubAndSum(values);
    }

    private Optional<PortfolioPropertyEntity> getTotalAssets(String portfolio, PortfolioPropertyType currency) {
        return portfolioPropertyRepository
                .findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(portfolio, currency.name());
    }

    private static String getCurrency(PortfolioPropertyEntity entity) {
        int length = entity.getProperty().length();
        return entity.getProperty().substring(length - 3, length);
    }

    private static BigDecimal parseTotalAssetsIfCan(PortfolioPropertyEntity entity) {
        try {
            return BigDecimal.valueOf(
                    Double.parseDouble(
                            entity.getValue()));
        } catch (Exception e) {
            log.error("Значение должно содержать число, сохранено {}", entity);
            return BigDecimal.ZERO;
        }
    }

    public Optional<BigDecimal> getTotalCash(Collection<String> portfolios) {
        Collection<BigDecimal> portfolioCashes = portfolios.stream()
                .map(portfolio -> portfolioPropertyRepository.findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(
                        portfolio, PortfolioPropertyType.CASH.name()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(AssetsAndCashService::groupByCurrency)
                .map(this::convertToRubAndSum)
                .collect(toList());
        if (portfolioCashes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(portfolioCashes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Returns summed cash values
     */
    // currency -> value
    private static Map<String, BigDecimal> groupByCurrency(PortfolioPropertyEntity e) {
        try {
            return PortfolioCash.deserialize(e.getValue())
                    .stream()
                    .collect(groupingBy(PortfolioCash::getCurrency,
                            reducing(BigDecimal.ZERO, PortfolioCash::getValue, BigDecimal::add)));
        } catch (Exception ex) {
            log.warn("Ошибка при десериализации свойства: {}", e.getValue(), ex);
            return Collections.emptyMap();
        }
    }

    /**
     * @param values map of currency -> value
     */
    private BigDecimal convertToRubAndSum(Map<String, BigDecimal> values) {
        return values.entrySet()
                .stream()
                .map(e -> foreignExchangeRateService.convertValueToCurrency(e.getValue(), e.getKey(), RUB))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
