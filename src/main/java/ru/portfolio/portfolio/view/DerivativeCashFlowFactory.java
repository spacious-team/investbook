/*
 * Portfolio
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

package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEventCashFlowConverter;
import ru.portfolio.portfolio.converter.TransactionCashFlowConverter;
import ru.portfolio.portfolio.converter.TransactionConverter;
import ru.portfolio.portfolio.pojo.*;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DerivativeCashFlowFactory {
    private static final ZoneId MOEX_TIMEZONE = ZoneId.of("Europe/Moscow");
    private static final int LAST_TRADE_HOUR = 18;
    private final TransactionRepository transactionRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final TransactionCashFlowRepository transactionCashFlowRepository;
    private final TransactionConverter transactionConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final TransactionCashFlowConverter transactionCashFlowConverter;

    public DerivativeCashFlow getDerivativeCashFlow(Portfolio portfolio, Security contract) {
        Deque<Transaction> transactions = getTransactions(portfolio, contract);
        Deque<SecurityEventCashFlow> securityEventCashFlows = getSecurityEventCashFlows(portfolio, contract);

        DerivativeCashFlow derivativeCashFlow = new DerivativeCashFlow();
        BigDecimal totalProfit = BigDecimal.ZERO;
        int currentPosition = 0;
        for (SecurityEventCashFlow cash : securityEventCashFlows) {
            LocalDate currentDay = ZonedDateTime.ofInstant(cash.getTimestamp(), MOEX_TIMEZONE).toLocalDate();
            totalProfit = totalProfit.add(cash.getValue());
            Deque<Transaction> dailyTransactions = getDailyTransactions(transactions, currentDay);
            currentPosition += dailyTransactions.stream()
                    .mapToInt(Transaction::getCount)
                    .sum();

            derivativeCashFlow.getCashFlows().add(
                    DerivativeCashFlow.DailyCashFlow.builder()
                            .dailyTransactions(getCashFlows(dailyTransactions))
                            .dailyProfit(cash)
                            .totalProfit(totalProfit)
                            .position(currentPosition)
                            .build());
        }
        return derivativeCashFlow;
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

    private Deque<SecurityEventCashFlow> getSecurityEventCashFlows(Portfolio portfolio, Security contract) {
        return securityEventCashFlowRepository
                    .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdOrderByTimestampAsc(
                            portfolio.getId(),
                            contract.getIsin(),
                            CashFlowType.DERIVATIVE_PROFIT.getId())
                    .stream()
                    .map(securityEventCashFlowConverter::fromEntity)
                    .collect(Collectors.toCollection(LinkedList::new));
    }

    private Deque<Transaction> getDailyTransactions(Deque<Transaction> transactions, LocalDate currentDay) {
        return transactions.stream()
                .filter(e -> {
                    LocalDateTime dateTime = ZonedDateTime.ofInstant(e.getTimestamp(), MOEX_TIMEZONE).toLocalDateTime();
                    return currentDay.equals((dateTime.get(ChronoField.HOUR_OF_DAY) <= LAST_TRADE_HOUR) ?
                            dateTime.toLocalDate() :
                            dateTime.toLocalDate().plusDays(1));
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> getCashFlows(Deque<Transaction> dailyTransactions) {
        LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactionsCashFlows = new LinkedHashMap<>();
        for (Transaction transaction : dailyTransactions) {
            if (transaction.getId() != null) {
                dailyTransactionsCashFlows.put(transaction, getTransactionCashFlows(transaction));
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
