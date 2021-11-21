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

package ru.investbook.report;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
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

import static org.spacious_team.broker.pojo.SecurityType.getCurrencyPair;

@Component
@RequiredArgsConstructor
public class FifoPositionsFactory {

    private static final String ALL_PORTFOLIO_KEY = "all";
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final Map<String, Map<String, FifoPositions>> positionsCache = new ConcurrentHashMap<>();

    public FifoPositions get(Security security, Portfolio portfolio) {
        return get(security, FifoPositionsFilter.of(portfolio));
    }

    public FifoPositions get(Security security, FifoPositionsFilter filter) {
        return get(security.getId(), filter);
    }

    public FifoPositions get(String isinOrContract, FifoPositionsFilter filter) {
        String key = filter.getPortfolios().stream().sorted().collect(Collectors.joining(","));
        return positionsCache
                .computeIfAbsent(
                        key.isEmpty() ? ALL_PORTFOLIO_KEY : key,
                        k -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        getCacheKey(isinOrContract, filter),
                        k -> create(isinOrContract, filter));
    }

    public void invalidateCache() {
        positionsCache.clear();
    }

    private String getCacheKey(String isinOrContract, FifoPositionsFilter filter) {
        String key = (SecurityType.getSecurityType(isinOrContract) == SecurityType.CURRENCY_PAIR) ?
                getCurrencyPair(isinOrContract) :
                isinOrContract;
        return key + filter.getFromDate().toString() + filter.getToDate().toString();
    }

    private FifoPositions create(String isinOrContract, FifoPositionsFilter filter) {
        SecurityType type = SecurityType.getSecurityType(isinOrContract);
        LinkedList<Transaction> transactions;
        if (type == SecurityType.CURRENCY_PAIR) {
            String currencyPair = getCurrencyPair(isinOrContract);
            transactions = getFxContracts(currencyPair, filter)
                    .stream()
                    .flatMap(contract -> getTransactions(contract, filter).stream())
                    .collect(Collectors.toCollection(LinkedList::new));
            transactions.sort(
                    Comparator.comparing(Transaction::getTimestamp)
                            .thenComparing(Transaction::getId));
        } else {
            transactions = getTransactions(isinOrContract, filter);
        }
        Deque<SecurityEventCashFlow> redemption = (type == SecurityType.STOCK_OR_BOND) ?
                getRedemption(isinOrContract, filter) :
                new ArrayDeque<>(0);
        return new FifoPositions(transactions, redemption);
    }

    private Collection<String> getFxContracts(String currencyPair, FifoPositionsFilter filter) {
        return filter.getPortfolios().isEmpty() ?
                transactionRepository
                        .findDistinctFxContractByCurrencyPairAndTimestampBetween(
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findDistinctFxContractByPortfolioInAndCurrencyPairAndTimestampBetween(
                                filter.getPortfolios(),
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate());
    }

    public LinkedList<Transaction> getTransactions(String isin, FifoPositionsFilter filter) {
        List<TransactionEntity> entities = filter.getPortfolios().isEmpty() ?
                transactionRepository
                        .findBySecurityIdAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
                                isin,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findBySecurityIdAndPortfolioInAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
                                isin,
                                filter.getPortfolios(),
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getRedemption(String isin, FifoPositionsFilter filter) {
        List<SecurityEventCashFlowEntity> entities = filter.getPortfolios().isEmpty() ?
                securityEventCashFlowRepository
                        .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                isin,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate()) :
                securityEventCashFlowRepository
                        .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                filter.getPortfolios(),
                                isin,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
