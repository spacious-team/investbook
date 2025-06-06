/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.util.Assert;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Objects.requireNonNull;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder(toBuilder = true)
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class TradePof {

    @JsonProperty("id")
    int id;

    @JsonProperty("trade-id")
    @NotEmpty
    String tradeId;

    /**
     * Дата и время сделки.
     * Если значение null, то значение поля settlement обязательно
     */
    @JsonProperty("timestamp")
    @Nullable
    Long timestamp;

    /**
     * Дата и время поставки.
     * Если значение null, то значение поля timestamp обязательно
     */
    @JsonProperty("settlement")
    @Nullable
    Long settlement;

    @JsonProperty("account")
    int account;

    @Getter(AccessLevel.NONE)
    @JsonProperty("asset")
    int asset;

    /**
     * Если '+', то это покупка, если '-', то это продажа.
     * Поддерживаются дробные акции
     */
    @JsonProperty("count")
    @NotNull
    BigDecimal count;

    /**
     * Цена бумаги/контракта (за единицу) в валюте сделки,
     * для облигации - без учета НКД, для деривативов поле может отсутствовать
     */
    @JsonProperty("price")
    @Nullable
    BigDecimal price;

    @JsonProperty("accrued-interest")
    @Nullable
    BigDecimal accruedInterest;

    /**
     * Котировка. Для облигации в процентах, для деривативов в пунктах.
     * Для деривативов - обязательное поле, для облигаций - опциональное.
     */
    @JsonProperty("quote")
    @Nullable
    BigDecimal quote;

    /**
     * Может отсутствовать, если поле 'price' и 'accrued-interest' отсутствуют
     */
    @JsonProperty("currency")
    @Nullable
    String currency;

    /**
     * Комиссия. Если отрицательное значение, значит возврат комиссии
     */
    @JsonProperty("fee")
    @NotNull
    BigDecimal fee;

    @JsonProperty("fee-currency")
    @NotEmpty
    String feeCurrency;

    @JsonProperty("description")
    @Nullable
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
                        .currency(getValidCurrencyOrNull(e.getCurrency())));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == ACCRUED_INTEREST.getId())
                .findAny()
                .ifPresent(e -> builder.accruedInterest(divide(e, count).abs()));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == DERIVATIVE_QUOTE.getId())
                .findAny()
                .ifPresent(e -> builder.quote(divide(e, count).abs()));
        transactionCashFlows.stream()
                .filter(e -> e.getCashFlowType().getId() == FEE.getId())
                .findAny()
                .ifPresentOrElse(e -> builder
                                .fee(e.getValue().negate())
                                .feeCurrency(requireNonNull(getValidCurrencyOrNull(e.getCurrency()))),
                        () -> builder.fee(BigDecimal.ZERO).feeCurrency("RUB"));
        return builder.build();
    }

    static TradePof of(SecurityEventCashFlowEntity redemption, int id) {
        Assert.isTrue(redemption.getCashFlowType().getId() == REDEMPTION.getId(),
                () -> "ожидается событие погашения облигации: " + redemption);
        long settlement = redemption.getTimestamp().getEpochSecond();
        int account = AccountPof.getAccountId(redemption.getPortfolio().getId());
        int count = -Math.abs(redemption.getCount());
        return TradePof.builder()
                .id(id)
                .tradeId(settlement + ":" + redemption.getSecurity().getId() + ":" + account)
                .settlement(settlement)
                .account(account)
                .asset(redemption.getSecurity().getId())
                .count(BigDecimal.valueOf(count))
                .price(divide(redemption))
                .currency(getValidCurrencyOrNull(redemption.getCurrency()))
                .fee(BigDecimal.ZERO)
                .feeCurrency("RUB")
                .build();
    }

    private static BigDecimal divide(TransactionCashFlowEntity e, int count) {
        return e.getValue().divide(BigDecimal.valueOf(Math.abs(count)), 6, HALF_UP);
    }

    private static BigDecimal divide(SecurityEventCashFlowEntity e) {
        return e.getValue().divide(BigDecimal.valueOf(Math.abs(e.getCount())), 6, HALF_UP);
    }

    Optional<AbstractTransaction> toTransaction(Map<Integer, String> accountToPortfolioId,
                                                Map<Integer, Integer> assetToSecurityId,
                                                Map<Integer, SecurityType> assetTypes) {
        try {
            SecurityType securityType = requireNonNull(assetTypes.get(asset));
            @Nullable BigDecimal value = (price == null) ? null : price.multiply(count).negate();
            AbstractTransaction.AbstractTransactionBuilder<?, ?> builder = switch (securityType) {
                case STOCK, BOND, STOCK_OR_BOND, ASSET -> SecurityTransaction.builder()
                        .value(requireNonNull(value))
                        .accruedInterest((accruedInterest == null) ? null : accruedInterest.multiply(count).negate())
                        .valueCurrency(requireNonNull(getValidCurrencyOrNull(currency)));
                case DERIVATIVE -> DerivativeTransaction.builder()
                                .valueInPoints(requireNonNull(quote).multiply(count).negate())
                                .value(value) // для деривативов - опциональное
                                .valueCurrency(getValidCurrencyOrNull(currency)); // для деривативов - опциональное
                case CURRENCY_PAIR -> ForeignExchangeTransaction.builder()
                        .value(requireNonNull(value))
                        .valueCurrency(requireNonNull(getValidCurrencyOrNull(currency)));
            };

            long ts = requireNonNull(getSettlementOrTimestamp());
            return Optional.of(builder
                    .tradeId(tradeId)
                    .portfolio(requireNonNull(accountToPortfolioId.get(account)))
                    .security(getSecurityId(assetToSecurityId))
                    .count(count.intValueExact())
                    .timestamp(Instant.ofEpochSecond(ts))
                    .fee(fee.negate())
                    .feeCurrency(requireNonNull(getValidCurrencyOrNull(feeCurrency)))
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    @Nullable
    Long getSettlementOrTimestamp() {
        return (settlement != null) ? settlement : timestamp;
    }

    int getSecurityId(Map<Integer, Integer> assetToSecurityId) {
        return Objects.requireNonNull(assetToSecurityId.get(asset));
    }
}
