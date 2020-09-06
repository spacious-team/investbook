/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.pojo.*;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ru.investbook.pojo.SecurityType.getCurrencyPair;

@Component
@RequiredArgsConstructor
public class PositionsFactory {

    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final Map<Portfolio, Map<String, Positions>> positionsCache = new ConcurrentHashMap<>();

    public Positions get(Portfolio portfolio, Security security, ViewFilter filter) {
        return get(portfolio, security.getIsin(), filter);
    }

    public Positions get(Portfolio portfolio, String isinOrContract, ViewFilter filter) {
        return positionsCache.computeIfAbsent(portfolio, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(getCacheKey(isinOrContract, filter), k -> create(portfolio, isinOrContract, filter));
    }

    private String getCacheKey(String isinOrContract, ViewFilter filter) {
        String key = (SecurityType.getSecurityType(isinOrContract) == SecurityType.CURRENCY_PAIR) ?
                getCurrencyPair(isinOrContract) :
                isinOrContract;
        return key + filter.getFromDate().toString() + filter.getToDate().toString();
    }

    private Positions create(Portfolio portfolio, String isinOrContract, ViewFilter filter) {
        SecurityType type = SecurityType.getSecurityType(isinOrContract);
        LinkedList<Transaction> transactions = new LinkedList<>();
        if (type == SecurityType.CURRENCY_PAIR) {
            String currencyPair = getCurrencyPair(isinOrContract);
            transactions.addAll(getTransactions(portfolio, currencyPair + "_TOM", filter));
            transactions.addAll(getTransactions(portfolio, currencyPair + "_TOD", filter));
            transactions.sort(
                    Comparator.comparing(Transaction::getTimestamp)
                            .thenComparingLong(Transaction::getId));
        } else {
            transactions.addAll(getTransactions(portfolio, isinOrContract, filter));
        }
        Deque<SecurityEventCashFlow> redemption = (type == SecurityType.STOCK_OR_BOND) ?
                getRedemption(portfolio, isinOrContract, filter) :
                new ArrayDeque<>(0);
        return new Positions(transactions, redemption);
    }

    private LinkedList<Transaction> getTransactions(Portfolio portfolio, String isin, ViewFilter filter) {
        return transactionRepository
                .findBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampAscPkIdAsc(
                        isin,
                        portfolio.getId(),
                        filter.getFromDate(),
                        filter.getToDate())
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<SecurityEventCashFlow> getRedemption(Portfolio portfolio, String isin, ViewFilter filter) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        portfolio.getId(),
                        isin,
                        CashFlowType.REDEMPTION.getId(),
                        filter.getFromDate(),
                        filter.getToDate())
                .stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
