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

import static java.util.Collections.singleton;
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

    public FifoPositions get(Portfolio portfolio, Security security, ViewFilter filter) {
        return get(singleton(portfolio.getId()), security.getId(), filter);
    }

    public FifoPositions get(Portfolio portfolio, String isinOrContract, ViewFilter filter) {
        return get(singleton(portfolio.getId()), isinOrContract, filter);
    }

    public FifoPositions get(Collection<String> portfolios, Security security, ViewFilter filter) {
        return get(portfolios, security.getId(), filter);
    }

    public FifoPositions get(Collection<String> portfolios, String isinOrContract, ViewFilter filter) {
        String key = portfolios.stream().sorted().collect(Collectors.joining(","));
        return positionsCache
                .computeIfAbsent(
                        key.isEmpty() ? ALL_PORTFOLIO_KEY : key,
                        k -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        getCacheKey(isinOrContract, filter),
                        k -> create(portfolios, isinOrContract, filter));
    }

    public void invalidateCache() {
        positionsCache.clear();
    }

    private String getCacheKey(String isinOrContract, ViewFilter filter) {
        String key = (SecurityType.getSecurityType(isinOrContract) == SecurityType.CURRENCY_PAIR) ?
                getCurrencyPair(isinOrContract) :
                isinOrContract;
        return key + filter.getFromDate().toString() + filter.getToDate().toString();
    }

    private FifoPositions create(Collection<String> portfolios, String isinOrContract, ViewFilter filter) {
        SecurityType type = SecurityType.getSecurityType(isinOrContract);
        LinkedList<Transaction> transactions;
        if (type == SecurityType.CURRENCY_PAIR) {
            String currencyPair = getCurrencyPair(isinOrContract);
            transactions = getFxContracts(portfolios, currencyPair, filter)
                    .stream()
                    .flatMap(contract -> getTransactions(portfolios, contract, filter).stream())
                    .collect(Collectors.toCollection(LinkedList::new));
            transactions.sort(
                    Comparator.comparing(Transaction::getTimestamp)
                            .thenComparing(Transaction::getId));
        } else {
            transactions = getTransactions(portfolios, isinOrContract, filter);
        }
        Deque<SecurityEventCashFlow> redemption = (type == SecurityType.STOCK_OR_BOND) ?
                getRedemption(portfolios, isinOrContract, filter) :
                new ArrayDeque<>(0);
        return new FifoPositions(transactions, redemption);
    }

    private Collection<String> getFxContracts(Collection<String> portfolios, String currencyPair, ViewFilter filter) {
        return portfolios.isEmpty() ?
                transactionRepository
                        .findDistinctFxContractByCurrencyPairAndTimestampBetween(
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findDistinctFxContractByPortfolioInAndCurrencyPairAndTimestampBetween(
                                portfolios,
                                currencyPair,
                                filter.getFromDate(),
                                filter.getToDate());
    }

    public LinkedList<Transaction> getTransactions(Collection<String> portfolios, String isin, ViewFilter filter) {
        List<TransactionEntity> entities = portfolios.isEmpty() ?
                transactionRepository
                        .findBySecurityIdAndTimestampBetweenOrderByTimestampAscPkIdAsc(
                                isin,
                                filter.getFromDate(),
                                filter.getToDate()) :
                transactionRepository
                        .findBySecurityIdAndPkPortfolioInAndTimestampBetweenOrderByTimestampAscPkIdAsc(
                                isin,
                                portfolios,
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getRedemption(Collection<String> portfolios, String isin, ViewFilter filter) {
        List<SecurityEventCashFlowEntity> entities = portfolios.isEmpty() ?
                securityEventCashFlowRepository
                        .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                isin,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate()) :
                securityEventCashFlowRepository
                        .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                portfolios,
                                isin,
                                CashFlowType.REDEMPTION.getId(),
                                filter.getFromDate(),
                                filter.getToDate());
        return entities.stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
