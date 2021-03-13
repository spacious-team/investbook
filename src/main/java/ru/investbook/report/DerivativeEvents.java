/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import lombok.Builder;
import lombok.Getter;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DerivativeEvents {
    private final List<DerivativeDailyEvents> derivativeDailyEvents = new ArrayList<>();

    @Getter
    @Builder
    public static class DerivativeDailyEvents {
        @Nullable
        private final SecurityEventCashFlow dailyProfit;
        @Nullable
        private final LinkedHashMap<Transaction, Map<CashFlowType, TransactionCashFlow>> dailyTransactions;
        private final BigDecimal totalProfit;
        private final int position;
    }
}
