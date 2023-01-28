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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.lang.Nullable;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.openformat.OpenFormatHelper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class PaymentPof {

    @NotNull
    @JsonProperty("id")
    int id;

    @Nullable
    @JsonProperty("payment-id")
    String paymentId;

    @NotNull
    @JsonProperty("account")
    int account;

    @NotNull
    @Getter(AccessLevel.NONE)
    @JsonProperty("asset")
    int asset;

    @NotNull
    @JsonProperty("type")
    PaymentTypePof type;

    /**
     * Поддерживаются дробные акции
     */
    @NotNull
    @JsonProperty("count")
    BigDecimal count;

    @NotNull
    @JsonProperty("timestamp")
    long timestamp;

    @NotNull
    @JsonProperty("amount")
    BigDecimal amount;

    @NotEmpty
    @JsonProperty("currency")
    String currency;

    @Nullable
    @JsonProperty("tax")
    BigDecimal tax;

    @Nullable
    @JsonProperty("tax-currency")
    String taxCurrency;

    @Nullable
    @JsonProperty("description")
    String description;

    static PaymentPof of(SecurityEventCashFlowEntity cashFlow, Optional<SecurityEventCashFlowEntity> tax) {
        return PaymentPof.builder()
                .id(cashFlow.getId())
                .account(AccountPof.getAccountId(cashFlow.getPortfolio().getId()))
                .asset(cashFlow.getSecurity().getId())
                .type(PaymentTypePof.valueOf(CashFlowType.valueOf(cashFlow.getCashFlowType().getId())))
                .count(cashFlow.getCount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(cashFlow.getCount())) // опциональное для деривативов
                .timestamp(cashFlow.getTimestamp().getEpochSecond())
                .amount(cashFlow.getValue())
                .currency(getValidCurrencyOrNull(cashFlow.getCurrency()))
                .tax(tax.map(SecurityEventCashFlowEntity::getValue).map(BigDecimal::negate).orElse(null))
                .taxCurrency(tax.map(SecurityEventCashFlowEntity::getCurrency)
                        .map(OpenFormatHelper::getValidCurrencyOrNull)
                        .orElse(null))
                .build();
    }

    Collection<SecurityEventCashFlow> getSecurityEventCashFlow(Map<Integer, String> accountToPortfolioId,
                                                               Map<Integer, Integer> assetToSecurityId,
                                                               Map<Integer, SecurityType> assetTypes) {
        try {
            SecurityType securityType = Objects.requireNonNull(assetTypes.get(asset));
            CashFlowType eventType = type.toCashFlowType();
            if (securityType == SecurityType.BOND && eventType == CashFlowType.DIVIDEND) {
                eventType = CashFlowType.COUPON; // izi-invest.ru fix: не различает дивиденды и купоны (type = other)
            }
            SecurityEventCashFlow cashFlow = SecurityEventCashFlow.builder()
                    .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                    .security(getSecurityId(assetToSecurityId))
                    .eventType(eventType)
                    .count(count.intValueExact())
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .value(amount)
                    .currency(getValidCurrencyOrNull(currency))
                    .build();
            if (tax != null && Math.abs(tax.floatValue()) > 0.0001) {
                return Set.of(cashFlow,
                        cashFlow.toBuilder()
                                .eventType(CashFlowType.TAX)
                                .value(tax.negate())
                                .currency(getValidCurrencyOrNull(taxCurrency))
                                .build());
            } else {
                return Set.of(cashFlow);
            }
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Collections.emptySet();
        }
    }

    int getSecurityId(Map<Integer, Integer> assetToSecurityId) {
        return Objects.requireNonNull(assetToSecurityId.get(asset));
    }
}
