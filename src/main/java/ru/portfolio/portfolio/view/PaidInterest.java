package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
class PaidInterest {
    private final HashMap<CashFlowType, Map<Position, BigDecimal>> paidInterest = new HashMap<>();

    Map<Position, BigDecimal> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    BigDecimal get(CashFlowType payType, Position position) {
        return this.get(payType).get(position);
    }
}
