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
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.converter.TransactionConverter;
import ru.investbook.pojo.*;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.TransactionCashFlowRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public DerivativeEvents getDerivativeEvents(Portfolio portfolio, Security contract) {
        Deque<Transaction> transactions = getTransactions(portfolio, contract);
        Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows = getSecurityEventCashFlows(portfolio, contract);

        LocalDate firstEventDate = getContractFirstEventDate(transactions, securityEventCashFlows);
        LocalDate lastEventDate = Optional.ofNullable(getContractLastEventDate(transactions, securityEventCashFlows))
                .orElse(firstEventDate);

        DerivativeEvents derivativeEvents = new DerivativeEvents();
        BigDecimal totalProfit = BigDecimal.ZERO;
        int currentPosition = 0;
        LocalDate currentDay = firstEventDate;
        while (currentDay != null && currentDay.compareTo(lastEventDate) <= 0) {
            Deque<Transaction> dailyTransactions = getDailyTransactions(transactions, currentDay);
            SecurityEventCashFlow cash = securityEventCashFlows.get(currentDay);
            if ((dailyTransactions != null && !dailyTransactions.isEmpty())
                    || (cash != null && !cash.getValue().equals(BigDecimal.ZERO))) {
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

    private LinkedList<Transaction> getTransactions(Portfolio portfolio, Security contract) {
        return transactionRepository
                .findBySecurityIsinAndPkPortfolioOrderByTimestampAscPkIdAsc(
                        contract.getIsin(),
                        portfolio.getId())
                .stream()
                .map(transactionConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private Map<LocalDate, SecurityEventCashFlow> getSecurityEventCashFlows(Portfolio portfolio, Security contract) {
        return securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdOrderByTimestampAsc(
                        portfolio.getId(),
                        contract.getIsin(),
                        CashFlowType.DERIVATIVE_PROFIT.getId())
                .stream()
                .map(securityEventCashFlowConverter::fromEntity)
                .collect(Collectors.toMap(e -> getTradeDay(e.getTimestamp()), Function.identity()));
    }

    private LocalDate getContractFirstEventDate(Deque<Transaction> transactions,
                                                Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows) {
        LocalDate firstTransactionDate, firstEventDate;
        firstEventDate = firstTransactionDate = Optional.ofNullable(transactions.peekFirst())
                .map(t -> ZonedDateTime.ofInstant(t.getTimestamp(), MOEX_TIMEZONE).toLocalDate())
                .orElse(null);
        ArrayList<LocalDate> cashFlows = new ArrayList<>(securityEventCashFlows.keySet());
        if (!cashFlows.isEmpty()) {
            Collections.sort(cashFlows);
            LocalDate firstCashFlowDate = cashFlows.get(0);
            firstEventDate = (firstTransactionDate == null || firstCashFlowDate.compareTo(firstTransactionDate) < 0) ?
                    firstCashFlowDate : firstTransactionDate;
        }
        return firstEventDate;
    }

    private LocalDate getContractLastEventDate(Deque<Transaction> transactions,
                                               Map<LocalDate, SecurityEventCashFlow> securityEventCashFlows) {
        LocalDate lastTransactionDate, lastEventDate;
        lastEventDate = lastTransactionDate = Optional.ofNullable(transactions.peekFirst())
                .map(t -> ZonedDateTime.ofInstant(t.getTimestamp(), MOEX_TIMEZONE).toLocalDate())
                .orElse(null);
        ArrayList<LocalDate> cashFlows = new ArrayList<>(securityEventCashFlows.keySet());
        if (!cashFlows.isEmpty()) {
            Collections.sort(cashFlows);
            LocalDate lastCashFlowDate = cashFlows.get(cashFlows.size() - 1);
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
                .findByPkPortfolioAndPkTransactionId(
                        transaction.getPortfolio(),
                        transaction.getId())
                .stream()
                .map(transactionCashFlowConverter::fromEntity)
                .collect(Collectors.toMap(TransactionCashFlow::getEventType, Function.identity()));
    }
}
