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
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static ru.portfolio.portfolio.pojo.CashFlowType.*;

@Component
@RequiredArgsConstructor
public class PaidInterestFactory {
    private static final CashFlowType[] PAY_TYPES =  new CashFlowType[]{COUPON, AMORTIZATION, DIVIDEND, TAX};
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;

    public PaidInterest create(String portfolio, Security security, Positions positions) {
        PaidInterest paidInterest = new PaidInterest();
        for (CashFlowType type : PAY_TYPES) {
            paidInterest.get(type).putAll(getPositionWithPayments(portfolio, security.getIsin(), positions, type));
        }
        return paidInterest;
    }

    private Map<Position, List<SecurityEventCashFlow>> getPositionWithPayments(String portfolio,
                                                                               String isin,
                                                                               Positions positions,
                                                                               CashFlowType event) {
        List<SecurityEventCashFlowEntity> accruedInterests = securityEventCashFlowRepository
                .findByPortfolioIdAndSecurityIsinAndCashFlowTypeIdOrderByTimestampAsc(portfolio, isin, event.getId());

        Map<Position, List<SecurityEventCashFlow>> payments = new HashMap<>();
        for (SecurityEventCashFlowEntity entity : accruedInterests) {
            SecurityEventCashFlow cash = securityEventCashFlowConverter.fromEntity(entity);
            BigDecimal payPerOne =  cash.getValue()
                    .divide(BigDecimal.valueOf(cash.getCount()), 6, RoundingMode.HALF_UP);

            Instant bookClosureDate = getBookClosureDate(positions.getPositionHistories(), entity);
            Deque<Position> paidPositions = getPayedPositions(positions.getClosedPositions(), bookClosureDate);
            paidPositions.addAll(getPayedPositions(positions.getOpenedPositions(), bookClosureDate));

            for (Position position : paidPositions) {
                int count = position.getCount();
                if (count <= 0) {
                    throw new IllegalArgumentException("Internal error: payment could not be done for short position");
                }
                BigDecimal pay = payPerOne
                        .multiply(BigDecimal.valueOf(count))
                        .setScale(2, RoundingMode.DOWN);
                payments.computeIfAbsent(position, key -> new ArrayList<>())
                        .add(cash.toBuilder()
                                .count(count)
                                .value(pay)
                                .build());
            }
        }
        return payments;
    }

    /**
     * @param positionHistories securities position (date in the past -> securities count)
     * @param payment dividend or bonds accrued interest payment
     * @return shares book closure (bonds accrued interest paying) date
     */
    private Instant getBookClosureDate(Deque<PositionHistory> positionHistories, SecurityEventCashFlowEntity payment) {
        Instant payDate = payment.getTimestamp(); // дата перечисления дивидендов/купонов Брокером
        int payForSecurities = payment.getCount();
        Iterator<PositionHistory> it = positionHistories.descendingIterator();
        // дата перечисления дивидендов/купонов Эмитентом (дата фиксации реестра акционеров)
        // с точностью до временного интервала между 2-мя соседними транзакции
        Instant bookClosureDate  = null;
        while (it.hasNext()) {
            PositionHistory positionHistory = it.next();
            Instant pastInstant = positionHistory.getInstant();
            if (payDate.isAfter(pastInstant) && (payForSecurities == positionHistory.getOpenedPositions())) {
                bookClosureDate  = pastInstant.plusNanos(1);
                break;
            }
        }
        if (bookClosureDate  == null) {
            throw new IllegalArgumentException("История транзакций для ЦБ " + payment.getSecurity().getIsin() +
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
}
