package ru.portfolio.portfolio.view;

import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Links securities events (dividends, bond amortizations) with transactions.
 */
@Getter
public class PaidInterest {
    private final HashMap<CashFlowType, Map<Position, List<SecurityEventCashFlow>>> paidInterest = new HashMap<>();

    Map<Position, List<SecurityEventCashFlow>> get(CashFlowType type) {
        return paidInterest.computeIfAbsent(type, k -> new HashMap<>());
    }

    public List<SecurityEventCashFlow> get(CashFlowType payType, Position position) {
        List<SecurityEventCashFlow> value = this.get(payType).get(position);
        return (value != null) ? value : Collections.emptyList();
    }
}
