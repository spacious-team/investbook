/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.SecurityEventCashFlowRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static org.spacious_team.broker.pojo.CashFlowType.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaidInterestFactory {
    private static final CashFlowType[] PAY_TYPES = new CashFlowType[]{COUPON, AMORTIZATION, DIVIDEND, TAX};
    private final FifoPositionsFactory positionsFactory;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;

    @Transactional
    public PaidInterest get(Portfolio portfolio, Security security, ViewFilter filter) {
        ViewFilter filterTillToDate = filter.toBuilder()
                .fromDate(ViewFilter.defaultFromDate) // the entire history of positions from the first transaction is required
                .build();
        FifoPositions positions = positionsFactory.get(portfolio, security, filterTillToDate);
        return create(portfolio.getId(), security, positions, filter);
    }

    private PaidInterest create(String portfolio, Security security, FifoPositions positions, ViewFilter filter) {
        PaidInterest paidInterest = new PaidInterest();
        for (CashFlowType type : PAY_TYPES) {
            paidInterest.get(type).putAll(getPositionWithPayments(portfolio, security.getId(), positions, type, filter));
        }
        return paidInterest;
    }

    private Map<Position, List<SecurityEventCashFlow>> getPositionWithPayments(String portfolio,
                                                                               String isin,
                                                                               FifoPositions positions,
                                                                               CashFlowType event,
                                                                               ViewFilter filter) {
        List<SecurityEventCashFlowEntity> eventCashFlowEntities = securityEventCashFlowRepository
                .findByPortfolioIdInAndSecurityIdAndCashFlowTypeIdAndTimestampBetweenOrderByTimestampAsc(
                        singleton(portfolio),
                        isin,
                        event.getId(),
                        filter.getFromDate(),
                        filter.getToDate());

        Map<Position, List<SecurityEventCashFlow>> payments = new HashMap<>();
        for (SecurityEventCashFlowEntity entity : eventCashFlowEntities) {
            SecurityEventCashFlow cash = securityEventCashFlowConverter.fromEntity(entity);
            try {
                Instant bookClosureDate = getBookClosureDate(positions.getPositionHistories(), entity);
                Deque<Position> paidPositions = getPayedPositions(positions.getClosedPositions(), bookClosureDate);
                paidPositions.addAll(getPayedPositions(positions.getOpenedPositions(), bookClosureDate));

                // filter only positions was opened in 'filter' interval
                paidPositions = paidPositions.stream()
                        .filter(position -> position.wasOpenedBetweenDates(filter.getFromDate(), filter.getToDate()))
                        .collect(Collectors.toCollection(LinkedList::new));

                getPayments(cash, paidPositions).forEach((position, cashs) ->
                        cashs.forEach(securityCash ->
                                payments.computeIfAbsent(position, p -> new ArrayList<>())
                                        .add(securityCash)));
            } catch (Exception e) {
                log.error("{}, выплата будет отображена в отчете по фиктивной позиции покупки ЦБ от даты {}",
                        e.getMessage(), PaidInterest.fictitiousPositionInstant, e);
                payments.computeIfAbsent(PaidInterest.getFictitiousPositionPayment(cash), key -> new ArrayList<>())
                        .add(cash);
            }
        }
        return payments;
    }

    /**
     * @param positionHistories securities position (date in the past -> securities count)
     * @param payment           dividend or bonds accrued interest payment
     * @return shares book closure (bonds accrued interest paying) date
     */
    private Instant getBookClosureDate(Deque<PositionHistory> positionHistories, SecurityEventCashFlowEntity payment) {
        Instant payDate = payment.getTimestamp(); // дата перечисления дивидендов/купонов Брокером
        int payForSecurities = payment.getCount();
        Iterator<PositionHistory> it = positionHistories.descendingIterator();
        // дата перечисления дивидендов/купонов Эмитентом (дата фиксации реестра акционеров)
        // с точностью до временного интервала между 2-мя соседними транзакции
        Instant bookClosureDate = null;
        while (it.hasNext()) {
            PositionHistory positionHistory = it.next();
            Instant pastInstant = positionHistory.getInstant();
            if (payDate.isAfter(pastInstant) && (payForSecurities == positionHistory.getOpenedPositions())) {
                bookClosureDate = pastInstant.plusNanos(1);
                break;
            }
        }
        if (bookClosureDate == null) {
            throw new IllegalArgumentException("История транзакций для ЦБ " + payment.getSecurity().getId() +
                    ((payment.getSecurity().getName() != null) ? " (\"" + payment.getSecurity().getName() + "\") " : " ") +
                    "не полная, не найден день в прошлом, " +
                    "в который количество открытых позиций равно " + payForSecurities +
                    ", по которым выполнена выплата купона/дивиденда");
        }
        return bookClosureDate;
    }

    private <T extends Position> Deque<Position> getPayedPositions(Deque<T> positions, Instant bookClosureDate) {
        Iterator<? extends T> it = positions.descendingIterator();
        Deque<Position> payedPositions = new LinkedList<>();
        while (it.hasNext()) {
            T position = it.next();
            if (position.wasOpenedAtTheInstant(bookClosureDate)) {
                payedPositions.addFirst(position);
            }
        }
        return payedPositions;
    }

    private Map<Position, List<SecurityEventCashFlow>> getPayments(SecurityEventCashFlow cash, Deque<Position> paidPositions) {
        Map<Position, List<SecurityEventCashFlow>> payments = new HashMap<>();
        BigDecimal payPerOne = cash.getValue().divide(BigDecimal.valueOf(cash.getCount()), 6, RoundingMode.HALF_UP);
        for (Position position : paidPositions) {
            int count = position.getCount();
            Assert.isTrue(count > 0, "Internal error: payment could not be done for short position");
            BigDecimal pay = payPerOne
                    .multiply(BigDecimal.valueOf(count))
                    .setScale(6, RoundingMode.HALF_UP);
            payments.computeIfAbsent(position, key -> new ArrayList<>())
                    .add(cash.toBuilder()
                            .count(count)
                            .value(pay)
                            .build());
        }
        return payments;
    }
}
