package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.util.*;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
class PaidInterest {
    private final HashMap<CashFlowType, Map<Position, List<BigDecimal>>> paidInterest = new HashMap<>();

    Map<Position, List<BigDecimal>> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    List<BigDecimal> get(CashFlowType payType, Position position) {
        List<BigDecimal> value = this.get(payType).get(position);
        return (value != null) ? value : Collections.emptyList();
    }
}
