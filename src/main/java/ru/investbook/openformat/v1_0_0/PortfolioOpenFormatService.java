/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.openformat.v1_0_0;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.PortfolioCashEntity;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioCashRepository;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PortfolioOpenFormatService {
    private final AssetsAndCashService assetsAndCashService;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final EventCashFlowRepository eventCashFlowRepository;
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioCashRepository portfolioCashRepository;

    public PortfolioOpenFormatV1_0_0 generate() {
        return PortfolioOpenFormatV1_0_0.builder()
                .end(getLatestEventTimestamp())
                .accounts(getAccounts())
                .cashBalances(getCashBalances())
                .assets(getAssets())
                .build();
    }

    private long getLatestEventTimestamp() {
        return Stream.of(
                        transactionRepository.findFirstByOrderByTimestampDesc()
                                .map(TransactionEntity::getTimestamp)
                                .orElse(Instant.EPOCH),
                        securityEventCashFlowRepository.findFirstByOrderByTimestampDesc()
                                .map(SecurityEventCashFlowEntity::getTimestamp)
                                .orElse(Instant.EPOCH),
                        eventCashFlowRepository.findFirstByOrderByTimestampDesc()
                                .map(EventCashFlowEntity::getTimestamp)
                                .orElse(Instant.EPOCH),
                        portfolioPropertyRepository.findFirstByOrderByTimestampDesc()
                                .map(PortfolioPropertyEntity::getTimestamp)
                                .orElse(Instant.EPOCH),
                        portfolioCashRepository.findFirstByOrderByTimestampDesc()
                                .map(PortfolioCashEntity::getTimestamp)
                                .orElse(Instant.EPOCH))
                .map(Instant::getEpochSecond)
                .max(Comparator.naturalOrder())
                .orElse(0L);
    }

    private List<AccountPof> getAccounts() {
        AccountPof.resetAccountIdGenerator();
        return portfolioRepository.findAll()
                .stream()
                .map(portfolio -> AccountPof.of(
                        portfolio,
                        assetsAndCashService.getTotalAssetsInRub(portfolio.getId()).orElse(BigDecimal.ZERO)))
                .toList();
    }

    private List<AssetPof> getAssets() {
        return securityRepository.findAll()
                .stream()
                .map(AssetPof::of)
                .toList();
    }

    private List<CashBalancesPof> getCashBalances() {
        return portfolioRepository.findAll()
                .stream()
                .map(this::getCashBalances)
                .toList();
    }

    private CashBalancesPof getCashBalances(PortfolioEntity portfolio) {
        List<PortfolioCashEntity> latestCashBalances = portfolioCashRepository
                .findDistinctOnPortfolioByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
                Set.of(portfolio.getId()),
                Instant.EPOCH,
                Instant.now());
        return CashBalancesPof.of(AccountPof.getAccountId(portfolio.getId()), latestCashBalances);
    }
}
