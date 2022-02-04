/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.openformat.v1_0_0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.lang.Nullable;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Collection;

import static java.math.RoundingMode.HALF_UP;
import static org.spacious_team.broker.pojo.CashFlowType.*;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradePof {

    @NotNull
    @JsonProperty("id")
    int id;

    @NotEmpty
    @JsonProperty("trade-id")
    String tradeId;

    /**
     * Если значение null, то значение поля settlement обязательно
     */
    @Nullable
    @JsonProperty("timestamp")
    Long timestamp;

    /**
     * Если значение null, то значение поля timestamp обязательно
     */
    @Nullable
    @JsonProperty("settlement")
    Long settlement;

    @NotNull
    @JsonProperty("account")
    int account;

    @NotNull
    @JsonProperty("asset")
    int asset;

    /**
     * Если '+', то это покупка, если '-', то это продажа
     * Поддерживаются дробные акции
     */
    @NotNull
    @JsonProperty("count")
    BigDecimal count;

    /**
     * В валюте сделки, для облигации - без НКД, для деривативов в валюте - опционально
     */
    @Nullable
    @JsonProperty("price")
    BigDecimal price;

    /**
     * Котировка. Для облигации в процентах, для деривативов в пунктах
     */
    @Nullable
    @JsonProperty("quote")
    BigDecimal quote;

    @Nullable
    @JsonProperty("accrued-interest")
    BigDecimal accruedInterest;

    @NotEmpty
    @JsonProperty("currency")
    String currency;

    /**
     * Комиссия. Если отрицательное значение, значит возврат комиссии
     */
    @NotNull
    @JsonProperty("fee")
    BigDecimal fee;

    @NotEmpty
    @JsonProperty("fee-currency")
    String feeCurrency;

    @Nullable
    @JsonProperty("description")
    String description;

    static TradePof of(TransactionEntity transaction,
                       Collection<TransactionCashFlowEntity> transactionCashFlows) {
        int count = transaction.getCount();
        TradePofBuilder builder = TradePof.builder()
                .id(transaction.getId())
                .tradeId(transaction.getTradeId())
                .settlement(transaction.getTimestamp().getEpochSecond())
                .account(AccountPof.getAccountId(transaction.getPortfolio()))
                .asset(transaction.getSecurity().getId())
                .count(BigDecimal.valueOf(count));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == PRICE.getId() ||
                        e.getCashFlowType().getId() == DERIVATIVE_PRICE.getId())
                .findAny()
                .ifPresent(e -> builder
                        .price(divide(e, count).abs())
                        .currency(e.getCurrency()));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == ACCRUED_INTEREST.getId())
                .findAny()
                .ifPresent(e -> builder.accruedInterest(divide(e, count).abs()));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == DERIVATIVE_QUOTE.getId())
                .findAny()
                .ifPresent(e -> builder.quote(divide(e, count).abs()));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == COMMISSION.getId())
                .findAny()
                .ifPresent(e -> builder
                        .fee(e.getValue().negate())
                        .feeCurrency(e.getCurrency()));
        return builder.build();
    }

    private static BigDecimal divide(TransactionCashFlowEntity e, int count) {
        return e.getValue().divide(BigDecimal.valueOf(Math.abs(count)), 4, HALF_UP);
    }
}
