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

import lombok.Getter;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.SecurityEventCashFlow;
import ru.investbook.pojo.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
public class PaidInterest {
    static final Instant fictitiousPositionInstant = Instant.ofEpochSecond(0);
    private final Map<CashFlowType, Map<Position, List<SecurityEventCashFlow>>> paidInterest = new HashMap<>();

    Map<Position, List<SecurityEventCashFlow>> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    public List<SecurityEventCashFlow> get(CashFlowType payType, Position position) {
        List<SecurityEventCashFlow> value = this.get(payType).get(position);
        return (value != null) ? value : Collections.emptyList();
    }

    public BigDecimal sumPaymentsForType(CashFlowType type) {
        return get(type)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .map(SecurityEventCashFlow::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static Position getFictitiousPositionPayment(SecurityEventCashFlow cash) {
        return new OpenedPosition(Transaction.builder()
                .timestamp(PaidInterest.fictitiousPositionInstant)
                .portfolio(cash.getPortfolio())
                .isin(cash.getIsin())
                .count(cash.getCount())
                .build());
    }

    /**
     * Positions with unknown transaction date
     * (for example dividend payment record exists but security transaction does not uploaded by broker report)
     */
    @SuppressWarnings("unchecked")
    public Deque<OpenedPosition> getFictitiousPositions() {
        Instant instant = fictitiousPositionInstant.plusNanos(1);
        return (Deque<OpenedPosition>)(Deque<?>) paidInterest.values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .filter(position -> position instanceof OpenedPosition)
                .filter(position -> position.wasOpenedAtTheInstant(instant))
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
