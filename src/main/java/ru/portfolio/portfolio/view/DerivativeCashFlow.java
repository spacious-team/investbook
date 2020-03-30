package ru.portfolio.portfolio.view;

import lombok.Builder;
import lombok.Getter;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.pojo.TransactionCashFlow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DerivativeCashFlow {
    private final List<DailyCashFlow> cashFlows = new ArrayList<>();

    @Getter
    @Builder
    public static class DailyCashFlow {
        private final SecurityEventCashFlow dailyProfit;
        private final LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactions;
        private final BigDecimal totalProfit;
        private final int position;
    }
}
