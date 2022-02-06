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

package ru.investbook.openformat.v1_1_0;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.lang.Nullable;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.math.RoundingMode.HALF_UP;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static org.spacious_team.broker.pojo.SecurityType.DERIVATIVE;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
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
     * Если '+', то это покупка, если '-', то это продажа.
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

    Optional<Transaction> toTransaction(Map<Integer, String> accountToPortfolioId) {
        try {
        long ts = Objects.requireNonNull((timestamp != null) ? timestamp : settlement);
        return Optional.of(Transaction.builder()
                .id(id)
                .tradeId(tradeId)
                .portfolio(Optional.of(accountToPortfolioId.get(account)).orElseThrow())
                .security(asset)
                .count(count.intValueExact())
                .timestamp(Instant.ofEpochSecond(ts))
                .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    Collection<TransactionCashFlow> getTransactionCashFlow(Map<Integer, SecurityType> assetTypes) {
        try {
        Collection<TransactionCashFlow> result = new ArrayList<>(3);
        SecurityType securityType = Objects.requireNonNull(assetTypes.get(asset));
        if (price != null) { // для деривативов - опциональное
            result.add(
                    TransactionCashFlow.builder()
                            .transactionId(id)
                            .eventType((securityType == DERIVATIVE) ? DERIVATIVE_PRICE : PRICE)
                            .value(price.multiply(count).negate())
                            .currency(currency)
                            .build());
        }
        if (accruedInterest != null) {
            result.add(
                    TransactionCashFlow.builder()
                            .transactionId(id)
                            .eventType(ACCRUED_INTEREST)
                            .value(accruedInterest.multiply(count).negate())
                            .currency(currency)
                            .build());
        }
        if (quote != null && securityType == DERIVATIVE) {
            result.add(
                    TransactionCashFlow.builder()
                            .transactionId(id)
                            .eventType(DERIVATIVE_QUOTE)
                            .value(quote.multiply(count).negate())
                            .currency("PNT")
                            .build());
        }
        if (fee != null) {
            result.add(
                    TransactionCashFlow.builder()
                            .transactionId(id)
                            .eventType(COMMISSION)
                            .value(fee.negate())
                            .currency(feeCurrency)
                            .build());
        }
        return result;
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Collections.emptyList();
        }
    }
}