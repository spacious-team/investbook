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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.stereotype.Component;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class DerivativeEventsFactory {
    private static final ZoneId MOEX_TIMEZONE = ZoneId.of("Europe/Moscow");
    private static final int LAST_TRADE_HOUR = 18;
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final TransactionCashFlowConverter transactionCashFlowConverter;

    public DerivativeEvents getDerivativeEvents(Portfolio portfolio, Security contract, ViewFilter filter) {
        Deque<Transaction> transactions = getTransactions(portfolio, contract, filter);
        Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows = getSecurityEventCashFlows(portfolio, contract, filter);

        @Nullable LocalDate firstEventDate = getContractFirstEventDate(transactions, securityEventCashFlows);
        @Nullable LocalDate lastEventDate = Optional.ofNullable(getContractLastEventDate(transactions, securityEventCashFlows))
                .orElse(firstEventDate);

        DerivativeEvents derivativeEvents = new DerivativeEvents();
        BigDecimal totalProfit = BigDecimal.ZERO;
        int currentPosition = 0;
        @Nullable LocalDate currentDay = firstEventDate;
        while (currentDay != null && currentDay.compareTo(lastEventDate) <= 0) {
            Deque<Transaction> dailyTransactions = getDailyTransactions(transactions, currentDay);
            @Nullable SecurityEventCashFlow cash = securityEventCashFlows.get(currentDay);
            //noinspection ConstantValue
            if ((dailyTransactions != null && !dailyTransactions.isEmpty())
                    || (cash != null && !cash.getValue().equals(BigDecimal.ZERO))) {
                //noinspection OptionalOfNullableMisuse
                currentPosition += Optional.ofNullable(dailyTransactions)
                        .orElse(new LinkedList<>())
                        .stream()
                        .mapToInt(Transaction::getCount)
                        .sum();
                totalProfit = totalProfit.add((cash == null) ? BigDecimal.ZERO : cash.getValue());

                derivativeEvents.getDerivativeDailyEvents().add(
                        DerivativeEvents.DerivativeDailyEvents.builder()
                                .dailyTransactions(getCashFlows(dailyTransactions))
                                .dailyProfit(cash)
                                .totalProfit(totalProfit)
                                .position(currentPosition)
                                .build());
            }
            currentDay = currentDay.plusDays(1);
        }
        return derivativeEvents;
    }

    private LinkedList<Transaction> getTransactions(Portfolio portfolio, Security contract, ViewFilter filter) {
        return transactionRepository
                .findBySecurityIdAndPortfolioInAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
                        requireNonNull(contract.getId()),
                        singleton(portfolio.getId()),
                        filter.getFromDate(),
                        filter.getToDate())
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Map<LocalDate, SecurityEventCashFlow> getSecurityEventCashFlows(Portfolio portfolio, Security contract, ViewFilter filter) {
        return securityEventCashFlowRepository
                .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        singleton(portfolio.getId()),
                        requireNonNull(contract.getId()),
                        CashFlowType.DERIVATIVE_PROFIT.getId(),
                        filter.getFromDate(),
                        filter.getToDate())
                .stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toMap(e -> getTradeDay(e.getTimestamp()), Function.identity()));
    }

    private @Nullable LocalDate getContractFirstEventDate(Deque<Transaction> transactions,
                                                Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows) {
        @Nullable LocalDate firstTransactionDate, firstEventDate;
        firstEventDate = firstTransactionDate = Optional.ofNullable(transactions.peekFirst())
                .map(t -> ZonedDateTime.ofInstant(t.getTimestamp(), MOEX_TIMEZONE).toLocalDate())
                .orElse(null);
        ArrayList<LocalDate> cashFlows = new ArrayList<>(securityEventCashFlows.keySet());
        if (!cashFlows.isEmpty()) {
            Collections.sort(cashFlows);
            @Nullable LocalDate firstCashFlowDate = cashFlows.getFirst();
            firstEventDate = (firstTransactionDate == null || firstCashFlowDate.compareTo(firstTransactionDate) < 0) ?
                    firstCashFlowDate : firstTransactionDate;
        }
        return firstEventDate;
    }

    private @Nullable LocalDate getContractLastEventDate(Deque<Transaction> transactions,
                                               Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows) {
        @Nullable LocalDate lastTransactionDate, lastEventDate;
        lastEventDate = lastTransactionDate = Optional.ofNullable(transactions.peekLast())
                .map(Transaction::getTimestamp)
                .map(DerivativeEventsFactory::getTradeDay)
                .orElse(null);
        ArrayList<LocalDate> cashFlows = new ArrayList<>(securityEventCashFlows.keySet());
        if (!cashFlows.isEmpty()) {
            Collections.sort(cashFlows);
            @Nullable LocalDate lastCashFlowDate = cashFlows.getLast();
            lastEventDate = (lastTransactionDate == null || lastCashFlowDate.compareTo(lastTransactionDate) > 0) ?
                    lastCashFlowDate : lastTransactionDate;
        }
        return lastEventDate;
    }

    private Deque<Transaction> getDailyTransactions(Deque<Transaction> transactions, LocalDate currentDay) {
        return transactions.stream()
                .filter(t -> getTradeDay(t.getTimestamp()).equals(currentDay))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * @return transaction day if transaction time less than 19-00 MSK, otherwise next day
     */
    private static LocalDate getTradeDay(Instant instant) {
        LocalDateTime dateTime = ZonedDateTime.ofInstant(instant, MOEX_TIMEZONE).toLocalDateTime();
        return (dateTime.get(ChronoField.HOUR_OF_DAY) <= LAST_TRADE_HOUR) ?
                dateTime.toLocalDate() :
                dateTime.toLocalDate().plusDays(1);
    }

    private LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> getCashFlows(Deque<Transaction> dailyTransactions) {
        LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactionsCashFlows = new LinkedHashMap<>();
        //noinspection ConstantValue
        if (dailyTransactions != null) {
            for (Transaction transaction : dailyTransactions) {
                if (transaction.getId() != null) {
                    dailyTransactionsCashFlows.put(transaction, getTransactionCashFlows(transaction));
                }
            }
        }
        return dailyTransactionsCashFlows;
    }

    private Map<CashFlowType, TransactionCashFlow> getTransactionCashFlows(Transaction transaction) {
        return transactionCashFlowRepository
                .findByTransactionId(requireNonNull(transaction.getId()))
                .stream()
                .map(transactionCashFlowConverter::fromEntity)
                .collect(Collectors.toMap(TransactionCashFlow::getEventType, Function.identity()));
    }
}
