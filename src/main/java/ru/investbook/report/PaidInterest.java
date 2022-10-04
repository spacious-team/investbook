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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
@Slf4j
public class PaidInterest {
    static final Instant fictitiousPositionInstant = Instant.ofEpochSecond(0);
    private static final Instant fictitiousPositionInstantPlus1Nanosec = fictitiousPositionInstant.plusNanos(1);
    private final Map<CashFlowType, Map<Position, List<SecurityEventCashFlow>>> paidInterest = new HashMap<>();

    Map<Position, List<SecurityEventCashFlow>> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    public List<SecurityEventCashFlow> get(CashFlowType payType, Position position) {
        List<SecurityEventCashFlow> value = this.get(payType).get(position);
        return (value != null) ? value : Collections.emptyList();
    }

    /**
     * Returns payments currencies
     */
    public Collection<String> getCurrencies() {
        return paidInterest.values()
                .stream()
                .flatMap(m -> m.values().stream())
                .flatMap(Collection::stream)
                .map(SecurityEventCashFlow::getCurrency)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    static Position getFictitiousPositionPayment(SecurityEventCashFlow cash) {
        return new OpenedPosition(Transaction.builder()
                .timestamp(PaidInterest.fictitiousPositionInstant)
                .portfolio(cash.getPortfolio())
                .security(cash.getSecurity())
                .count(cash.getCount())
                .build());
    }

    /**
     * Positions with unknown transaction date
     * (for example dividend payment record exists but security transaction does not upload by broker report)
     */
    public Deque<OpenedPosition> getFictitiousPositions() {
        return paidInterest.values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .filter(position -> position instanceof OpenedPosition)
                .filter(PaidInterest::isFictitiousPosition)
                .map(position -> (OpenedPosition) position)
                //  get(CashFlowType, Position) returns both DIVIDEND and TAX events for each of equals positions,
                //  so do not return 2 positions (for DIVIDEND and TAX) for preventing duplicate during report building
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * If dividend, coupon or amortization record is exists, but no open transaction,
     * {@link PaidInterestFactory} creates fictitious open transaction.
     * @return true is if checking transaction is fictitious
     */
    public static boolean isFictitiousPosition(Position position) {
        return position.wasOpenedAtTheInstant(fictitiousPositionInstantPlus1Nanosec);
    }

    /**
     * If dividend, coupon or amortization record is exists, but not open transaction,
     * {@link PaidInterestFactory} creates fictitious open transaction.
     * @return true is if checking transaction is fictitious
     */
    public static boolean isFictitiousPositionTransaction(Transaction transaction) {
        return transaction.getId() == null;
    }
}
