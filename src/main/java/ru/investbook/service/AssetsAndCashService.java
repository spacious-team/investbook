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
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.report.FifoPositionsFilter;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.ControllerHelper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import static java.lang.System.nanoTime;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetsAndCashService {
    private final Set<SecurityType> stockBondAndAssetTypes = Set.of(STOCK, BOND, ASSET);
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final InvestmentProportionService investmentProportionService;
    private final SecurityProfitService securityProfitService;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;

    public Set<String> getActivePortfolios() {
        return ControllerHelper.getActivePortfolios(portfolioRepository);
    }

    public Optional<BigDecimal> getAssets(Collection<String> portfolios) {
        Collection<BigDecimal> portfolioAssets = portfolios.stream()
                .map(this::getTotalAssetsInRub)
                .flatMap(Optional::stream)
                .toList();
        if (portfolioAssets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(portfolioAssets.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));

    }

    public Optional<BigDecimal> getTotalAssetsInRub(String portfolio) {
        return getTotalAssetsByBrokerEstimationInRub(portfolio)
                .or(() -> getTotalAssetsByCurrentOrLastTransactionQuoteEstimationInRub(portfolio));
    }

    private Optional<BigDecimal> getTotalAssetsByBrokerEstimationInRub(String portfolio) {
        Collection<PortfolioPropertyEntity> assets = new ArrayList<>(2);
        getTotalAssets(portfolio, PortfolioPropertyType.TOTAL_ASSETS_RUB).ifPresent(assets::add);
        getTotalAssets(portfolio, PortfolioPropertyType.TOTAL_ASSETS_USD).ifPresent(assets::add);
        // groups by date
        TreeMap<Instant, List<PortfolioPropertyEntity>> assetsGroupedByDate = assets.stream()
                .collect(groupingBy(PortfolioPropertyEntity::getTimestamp, TreeMap::new, toList()));
        // finds last day assets
        return Optional.ofNullable(assetsGroupedByDate.lastEntry()) // finds last date assets
                .map(Map.Entry::getValue)
                .map(Collection::stream)
                .map(stream -> stream.map(e -> foreignExchangeRateService.convertValueToCurrency(
                                parseTotalAssetsIfCan(e), getCurrency(e), RUB))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
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

    private Optional<BigDecimal> getTotalAssetsByCurrentOrLastTransactionQuoteEstimationInRub(String portfolio) {
        try {
            long t0 = nanoTime();
            FifoPositionsFilter filter = FifoPositionsFilter.of(portfolio);
            BigDecimal assetsInRub = securityRepository.findByTypeIn(stockBondAndAssetTypes)
                    .stream()
                    .map(securityConverter::fromEntity)
                    .map(security -> investmentProportionService
                            .getOpenedPositionsCostByCurrentOrLastTransactionQuoteInRub(security, filter))
                    .flatMap(Optional::stream)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assetsInRub = Objects.equals(assetsInRub, BigDecimal.ZERO) ? null : assetsInRub;
            log.info("Оценена стоимость активов по котировкам за {}", Duration.ofNanos(nanoTime() - t0));
            return Optional.ofNullable(assetsInRub)
                    .map(openedPositionCost -> openedPositionCost.add(
                            getTotalCashInRub(Set.of(portfolio))
                                    .orElse(BigDecimal.ZERO)));
        } catch (Exception e) {
            String message = "Ошибка оценки стоимости активов по котировкам";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public Optional<BigDecimal> getTotalCashInRub(Collection<String> portfolios) {
        Collection<BigDecimal> portfolioCashes =  securityProfitService.getPortfolioCash(portfolios, Instant.now())
                .stream()
                .map(cash -> foreignExchangeRateService.convertValueToCurrency(cash.getValue(), cash.getCurrency(), RUB))
                .toList();
        if (portfolioCashes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(portfolioCashes.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
