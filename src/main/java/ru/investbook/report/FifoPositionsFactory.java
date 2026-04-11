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

package ru.investbook.report;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Account;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.repository.TransactionRepository;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.spacious_team.broker.pojo.SecurityType.CURRENCY_PAIR;

@Component
@RequiredArgsConstructor
public class FifoPositionsFactory {

    private static final String ALL_ACCOUNT_KEY = "all";
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityRepository securityRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    // accounts -> cache_key -> positions
    private final Map<String, Map<String, FifoPositions>> positionsCache = new ConcurrentHashMap<>();

    public FifoPositions get(Security security, Account account) {
        return get(security, FifoPositionsFilter.of(account));
    }

    public FifoPositions get(Security security, FifoPositionsFilter filter) {
        int securityId = requireNonNull(security.getId());
        return get(securityId, security.getType(), filter);
    }

    /**
     * @param currencyPair in USDRUB format
     */
    public FifoPositions getForCurrencyPair(String currencyPair, FifoPositionsFilter filter) {
        return getAccountCache(filter).computeIfAbsent(
                getCacheKey(currencyPair, filter),
                _ -> create(currencyPair, filter));
    }

    public FifoPositions get(int securityId, SecurityType securityType, FifoPositionsFilter filter) {
        if (securityType == CURRENCY_PAIR) {
            String currencyPair = securityRepository.findCurrencyPair(securityId)
                    .orElseThrow(() -> new IllegalArgumentException("Валютная пара не найдена по id = " + securityId));
            return getForCurrencyPair(currencyPair, filter);
        }
        return getAccountCache(filter).computeIfAbsent(
                getCacheKey(String.valueOf(securityId), filter),
                _ -> create(securityId, securityType, filter));
    }

    private Map<String, FifoPositions> getAccountCache(FifoPositionsFilter filter) {
        String key = filter.getAccounts().stream().sorted().collect(Collectors.joining(","));
        return positionsCache.computeIfAbsent(
                key.isEmpty() ? ALL_ACCOUNT_KEY : key,
                _ -> new ConcurrentHashMap<>());
    }

    private String getCacheKey(String currencyPair, FifoPositionsFilter filter) {
        return currencyPair + filter.getFromDate() + filter.getToDate();
    }

    public void invalidateCache() {
        positionsCache.clear();
    }

    private FifoPositions create(String currencyPair, FifoPositionsFilter filter) {
        LinkedList<Transaction> transactions = getFxContracts(currencyPair, filter)
                .stream()
                .map(contract -> getTransactions(contract, filter))
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Transaction::getTimestamp).thenComparing(Transaction::getId))
                .collect(Collectors.toCollection(LinkedList::new));

        return new FifoPositions(transactions, new ArrayDeque<>(0));
    }

    private FifoPositions create(Integer securityId, SecurityType type, FifoPositionsFilter filter) {
        LinkedList<Transaction> transactions = getTransactions(securityId, filter);
        Deque<SecurityEventCashFlow> redemption = type.isBond() ?
                getRedemption(securityId, filter) :
                new ArrayDeque<>(0);
        return new FifoPositions(transactions, redemption);
    }

    private Collection<Integer> getFxContracts(String currencyPair, FifoPositionsFilter filter) {
        return filter.getAccounts().isEmpty() ?
                transactionRepository
                        .findDistinctFxContractByCurrencyPairAndTimestampBetween(
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findDistinctFxContractByAccountInAndCurrencyPairAndTimestampBetween(
                                filter.getAccounts(),
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate());
    }

    public LinkedList<Transaction> getTransactions(Integer securityId, FifoPositionsFilter filter) {
        List<TransactionEntity> entities = filter.getAccounts().isEmpty() ?
                transactionRepository
                        .findBySecurityIdAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
                                securityId,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findBySecurityIdAndAccountInAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
                                securityId,
                                filter.getAccounts(),
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getRedemption(Integer securityId, FifoPositionsFilter filter) {
        List<SecurityEventCashFlowEntity> entities = filter.getAccounts().isEmpty() ?
                securityEventCashFlowRepository
                        .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                securityId,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate()) :
                securityEventCashFlowRepository
                        .findByAccountIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                filter.getAccounts(),
                                securityId,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
