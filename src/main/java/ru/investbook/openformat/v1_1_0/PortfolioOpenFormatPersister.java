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

package ru.investbook.openformat.v1_1_0;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.SecurityDescription;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.investbook.parser.InvestbookApiClient;
import ru.investbook.parser.SecurityRegistrar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.Executors.newWorkStealingPool;
import static java.util.stream.Collectors.toMap;
import static ru.investbook.openformat.v1_1_0.PortfolioOpenFormatV1_1_0.GENERATED_BY_INVESTBOOK;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioOpenFormatPersister {
    private static final int TRADE_ID_MAX_LENGTH = 32; // investbook storage limit
    private final InvestbookApiClient api;
    private final SecurityRegistrar securityRegistrar;

    public void persist(PortfolioOpenFormatV1_1_0 object) {
        Collection<Runnable> tasks = new ArrayList<>();

        object.getAccounts()
                .stream()
                .map(AccountPof::toPortfolio)
                .flatMap(Optional::stream)
                .forEach(api::addPortfolio);

        Map<Integer, Integer> assetToSecurityId = object.getAssets()
                .parallelStream()
                .map(this::storeAndGetSecurityIdentifierMap)
                .flatMap(Optional::stream)
                .collect(toMap(SecurityIdentifierMap::assetId, SecurityIdentifierMap::securityId));

        Map<Integer, String> accountToPortfolioId = object.getAccounts()
                .stream()
                .collect(toMap(
                        AccountPof::getId,
                        a -> Optional.ofNullable(a.getAccountNumber()).orElse(String.valueOf(a.getId()))));
        Map<Integer, SecurityType> assetTypes = object.getAssets()
                .stream()
                .collect(toMap(AssetPof::getId, AssetPof::getSecurityType));

        tasks.add(() -> getTradesWithUniqTradeId(object.getTrades(), assetToSecurityId)
                .parallelStream()
                .map(t -> t.toTransaction(accountToPortfolioId, assetToSecurityId, assetTypes))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction));

        tasks.add(() -> getTransfersWithUniqTransferId(object.getTransfer(), assetToSecurityId)
                .parallelStream()
                .map(t -> t.toTransaction(accountToPortfolioId, assetToSecurityId))
                .flatMap(Optional::stream)
                .forEach(api::addTransaction));

        tasks.add(() -> object.getTransfer()
                .parallelStream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId, assetToSecurityId))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow));

        tasks.add(() -> object.getPayments()
                .parallelStream()
                .map(t -> t.getSecurityEventCashFlow(accountToPortfolioId, assetToSecurityId, assetTypes))
                .flatMap(Collection::stream)
                .forEach(api::addSecurityEventCashFlow));

        tasks.add(() -> object.getCashFlows()
                .parallelStream()
                .map(c -> c.toEventCashFlow(accountToPortfolioId))
                .flatMap(Optional::stream)
                .forEach(api::addEventCashFlow));

        VndInvestbookPof vndInvestbook = object.getVndInvestbook();
        if (vndInvestbook != null) {
            tasks.add(() -> vndInvestbook.getPortfolioCash().forEach(api::addPortfolioCash));
            tasks.add(() -> vndInvestbook.getPortfolioProperties().forEach(api::addPortfolioProperty));
            tasks.add(() -> vndInvestbook.getSecurityDescriptions()
                    .forEach(security -> persistSecurityDescription(security, assetToSecurityId)));
            tasks.add(() -> vndInvestbook.getSecurityQuotes()
                    .forEach(quote -> persistSecurityQuote(quote, assetToSecurityId)));
        }

        if (!Objects.equals(object.getGeneratedBy(), GENERATED_BY_INVESTBOOK)) {
            tasks.add(() -> persistTotalAssetsAndPortfolioCash(object, accountToPortfolioId));
        }

        runTasks(tasks);
    }

    @SneakyThrows
    private void runTasks(Collection<Runnable> tasks) {
        ExecutorService executorService = newWorkStealingPool(4 * Runtime.getRuntime().availableProcessors());
        try {
            Collection<Callable<Object>> callables = tasks.stream()
                    .map(Executors::callable)
                    .toList();
            executorService.invokeAll(callables);
        } finally {
            executorService.shutdown();
        }
    }

    private Optional<SecurityIdentifierMap> storeAndGetSecurityIdentifierMap(AssetPof asset) {
        return asset.toSecurity()
                .map(securityRegistrar::declareSecurity)
                .map(securityId -> new SecurityIdentifierMap(asset.getId(), securityId));
    }

    private record SecurityIdentifierMap(int assetId, int securityId) {
    }

    private Collection<TradePof> getTradesWithUniqTradeId(Collection<TradePof> trades,
                                                          Map<Integer, Integer> assetToSecurityId) {
        Collection<TradePof> tradesWithUniqId = new ArrayList<>(trades.size());
        Set<String> tradeIds = new HashSet<>(trades.size());
        for (TradePof t : trades) {
            String tradeId = StringUtils.hasText(t.getTradeId()) ?
                    t.getTradeId() :
                    t.getSettlementOrTimestamp() + ":" + t.getSecurityId(assetToSecurityId) + ":" + t.getAccount();
            String tid = getUniqId(tradeId, tradeIds);
            if (!Objects.equals(t.getTradeId(), tid)) {
                t = t.toBuilder().tradeId(tid).build();
            }
            tradesWithUniqId.add(t);
        }
        return tradesWithUniqId;
    }

    private Collection<TransferPof> getTransfersWithUniqTransferId(Collection<TransferPof> transfers,
                                                                   Map<Integer, Integer> assetToSecurityId) {
        Collection<TransferPof> transfersWithUniqId = new ArrayList<>(transfers.size());
        Set<String> transferIds = new HashSet<>(transfers.size());
        for (TransferPof t : transfers) {
            String transferId = StringUtils.hasText(t.getTransferId()) ?
                    t.getTransferId() :
                    t.getTimestamp() + ":" + t.getSecurityId(assetToSecurityId) + ":" + t.getAccount();
            String tid = getUniqId(transferId, transferIds);
            if (!Objects.equals(t.getTransferId(), tid)) {
                t = t.toBuilder().transferId(tid).build();
            }
            transfersWithUniqId.add(t);
        }
        return transfersWithUniqId;
    }

    private static String getUniqId(String id, Set<String> ids) {
        int cnt = 1;
        String tid = id.substring(0, Math.min(TRADE_ID_MAX_LENGTH, id.length()));
        while (!ids.add(tid)) {
            tid = concatWithLengthLimit(id, ++cnt);
        }
        return tid;
    }

    private static String concatWithLengthLimit(String value1, int value2) {
        int value2Digits = (int) (Math.ceil(Math.log(value2) + 1e-6)); // 1e-6 for 3 digits value2, ex. 100, log(100) = 2
        int value1AllowedLength = Math.min(TRADE_ID_MAX_LENGTH - value2Digits, value1.length());
        return value1.substring(0, value1AllowedLength) + value2;
    }

    private void persistSecurityDescription(SecurityDescription security, Map<Integer, Integer> assetToSecurityId) {
        int assetId = security.getSecurity();
        security = security.toBuilder()
                .security(getSecurityId(assetToSecurityId, assetId))
                .build();
        api.addSecurityDescription(security);
    }

    private void persistSecurityQuote(SecurityQuote quote, Map<Integer, Integer> assetToSecurityId) {
        int assetId = quote.getSecurity();
        quote = quote.toBuilder()
                .security(getSecurityId(assetToSecurityId, assetId))
                .build();
        api.addSecurityQuote(quote);
    }

    private static int getSecurityId(Map<Integer, Integer> assetToSecurityId, int asset) {
        return Objects.requireNonNull(assetToSecurityId.get(asset));
    }

    private void persistTotalAssetsAndPortfolioCash(PortfolioOpenFormatV1_1_0 object,
                                                    Map<Integer, String> accountToPortfolioId) {
        try {
            Instant end = Instant.ofEpochSecond(object.getEnd());
            object.getCashBalances()
                    .stream()
                    .map(cash -> cash.toPortfolioCash(accountToPortfolioId, end))
                    .flatMap(Collection::stream)
                    .forEach(api::addPortfolioCash);
            object.getAccounts()
                    .stream()
                    .map(a -> a.toTotalAssets(end))
                    .flatMap(Optional::stream)
                    .forEach(api::addPortfolioProperty);
        } catch (Exception e) {
            log.error("Не могу сохранить оценку активов или остаток денежных средств", e);
        }
    }
}
