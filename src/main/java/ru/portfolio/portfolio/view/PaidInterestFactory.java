package ru.portfolio.portfolio.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
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

    PaidInterest getPayedInterestFor(String portfolio, Security security, Positions positions) {
        PaidInterest paidInterest = new PaidInterest();
        for (CashFlowType type : PAY_TYPES) {
            paidInterest.get(type).putAll(getPositionWithPayments(portfolio, security.getIsin(), positions, type));
        }
        return paidInterest;
    }

    private Map<Position, List<BigDecimal>> getPositionWithPayments(String portfolio, String isin, Positions positions, CashFlowType event) {
        List<SecurityEventCashFlowEntity> accruedInterests = securityEventCashFlowRepository
                .findByPortfolioAndIsinAndCashFlowTypeOrderByTimestampAsc(portfolio, isin, event);

        Map<Position, List<BigDecimal>> payments = new HashMap<>();
        for (SecurityEventCashFlowEntity cash : accruedInterests) {
            BigDecimal payPerOne =  cash.getValue()
                    .divide(BigDecimal.valueOf(cash.getCount()), 6, RoundingMode.HALF_UP);

            Instant bookClosureDate = getBookClosureDate(positions.getPositionHistories(), cash);
            Deque<Position> payedPositions = getPayedPositions(positions.getClosedPositions(), bookClosureDate);
            payedPositions.addAll(getPayedPositions(positions.getOpenedPositions(), bookClosureDate));

            for (Position position : payedPositions) {
                int count = position.getCount();
                if (count <= 0) {
                    throw new IllegalArgumentException("Internal error: payment could not be done for short position");
                }
                BigDecimal pay = payPerOne
                        .multiply(BigDecimal.valueOf(count))
                        .setScale(2, RoundingMode.HALF_UP);
                payments.computeIfAbsent(position, key -> new ArrayList<>()).add(pay);
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
        Deque<Position> payedClosedPositions = new LinkedList<>();
        while (it.hasNext()) {
            T position = it.next();
            if (position.wasOpenedAtTheInstant(bookClosureDate)) {
                payedClosedPositions.addFirst(position);
            }
        }
        return payedClosedPositions;
    }
}
