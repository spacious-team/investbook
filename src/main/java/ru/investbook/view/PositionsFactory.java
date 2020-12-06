/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.view;

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
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.SecurityType.getCurrencyPair;

@Component
@RequiredArgsConstructor
public class PositionsFactory {

    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final Map<String, Map<String, Positions>> positionsCache = new ConcurrentHashMap<>();

    public Positions get(Portfolio portfolio, Security security, ViewFilter filter) {
        return get(
                (portfolio != Portfolio.ALL) ? Optional.of(portfolio) : Optional.empty(),
                security.getId(),
                filter);
    }

    public Positions get(Optional<Portfolio> portfolio, Security security, ViewFilter filter) {
        return get(portfolio, security.getId(), filter);
    }

    public Positions get(Portfolio portfolio, String isinOrContract, ViewFilter filter) {
        return get(Optional.of(portfolio), isinOrContract, filter);
    }

    public Positions get(Optional<Portfolio> portfolio, String isinOrContract, ViewFilter filter) {
        return positionsCache
                .computeIfAbsent(
                        portfolio.map(Portfolio::getId)
                                .orElse(Portfolio.ALL.getId()),
                        k -> new ConcurrentHashMap<>())
                .computeIfAbsent(
                        getCacheKey(isinOrContract, filter),
                        k -> create(portfolio, isinOrContract, filter));
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

    private Positions create(Optional<Portfolio> portfolio, String isinOrContract, ViewFilter filter) {
        SecurityType type = SecurityType.getSecurityType(isinOrContract);
        LinkedList<Transaction> transactions;
        if (type == SecurityType.CURRENCY_PAIR) {
            String currencyPair = getCurrencyPair(isinOrContract);
            transactions = getFxContracts(portfolio, currencyPair, filter)
                    .stream()
                    .flatMap(contract -> getTransactions(portfolio, contract, filter).stream())
                    .collect(Collectors.toCollection(LinkedList::new));
            transactions.sort(
                    Comparator.comparing(Transaction::getTimestamp)
                            .thenComparing(Transaction::getId));
        } else {
            transactions = getTransactions(portfolio, isinOrContract, filter);
        }
        Deque<SecurityEventCashFlow> redemption = (type == SecurityType.STOCK_OR_BOND) ?
                getRedemption(portfolio, isinOrContract, filter) :
                new ArrayDeque<>(0);
        return new Positions(transactions, redemption);
    }

    private Collection<String> getFxContracts(Optional<Portfolio> portfolio, String currencyPair, ViewFilter filter) {
        return portfolio
                .map(value ->
                        transactionRepository
                                .findDistinctFxContractByPortfolioAndCurrencyPairAndTimestampBetween(
                                        value,
                                        currencyPair,
                                        filter.getFromDate(),
                                        filter.getToDate()))
                .orElseGet(() ->
                        transactionRepository
                                .findDistinctFxContractByCurrencyPairAndTimestampBetween(
                                        currencyPair,
                                        filter.getFromDate(),
                                        filter.getToDate()));
    }

    private LinkedList<Transaction> getTransactions(Optional<Portfolio> portfolio, String isin, ViewFilter filter) {
        return portfolio
                .map(value ->
                        transactionRepository
                                .findBySecurityIdAndPkPortfolioAndTimestampBetweenOrderByTimestampAscPkIdAsc(
                                        isin,
                                        value.getId(),
                                        filter.getFromDate(),
                                        filter.getToDate()))
                .orElseGet(() ->
                        transactionRepository
                                .findBySecurityIdAndTimestampBetweenOrderByTimestampAscPkIdAsc(
                                        isin,
                                        filter.getFromDate(),
                                        filter.getToDate()))
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getRedemption(Optional<Portfolio> portfolio, String isin, ViewFilter filter) {
        return portfolio
                .map(value ->
                        securityEventCashFlowRepository
                                .findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                        value.getId(),
                                        isin,
                                        CashFlowType.REDEMPTION.getId(),
                                        filter.getFromDate(),
                                        filter.getToDate()))
                .orElseGet(() ->
                        securityEventCashFlowRepository
                                .findBySecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                                        isin,
                                        CashFlowType.REDEMPTION.getId(),
                                        filter.getFromDate(),
                                        filter.getToDate()))
                .stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
