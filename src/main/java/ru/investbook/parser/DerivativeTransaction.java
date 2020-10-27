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

package ru.investbook.parser;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.Transaction;
import ru.investbook.pojo.TransactionCashFlow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@EqualsAndHashCode
public class DerivativeTransaction {

    public static final String QUOTE_CURRENCY = "PNT"; // point
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final long transactionId;
    private final String portfolio;
    private final String contract;
    private final Instant timestamp;
    private final int count;
    private final BigDecimal value; // оценочная стоиомсть в валюце цены
    private final BigDecimal commission;
    private final String valueCurrency; // валюта платежа
    private final String commissionCurrency; // валюта коммиссии

    public Transaction getTransaction() {
        return Transaction.builder()
                .id(transactionId)
                .portfolio(portfolio)
                .isin(contract)
                .timestamp(timestamp)
                .count(count)
                .build();
    }

    public List<TransactionCashFlow> getTransactionCashFlows() {
        List<TransactionCashFlow> list = new ArrayList<>(2);
        list.add(TransactionCashFlow.builder()
                .transactionId(transactionId)
                .portfolio(portfolio)
                .eventType(valueCurrency.equals(QUOTE_CURRENCY) ?
                        CashFlowType.DERIVATIVE_QUOTE :
                        CashFlowType.DERIVATIVE_PRICE)
                .value(value)
                .currency(valueCurrency)
                .build());

        if (commission.abs().compareTo(minValue) >= 0) {
            list.add(TransactionCashFlow.builder()
                    .transactionId(transactionId)
                    .portfolio(portfolio)
                    .eventType(CashFlowType.COMMISSION)
                    .value(commission)
                    .currency(commissionCurrency)
                    .build());
        }
        return list;
    }
}
