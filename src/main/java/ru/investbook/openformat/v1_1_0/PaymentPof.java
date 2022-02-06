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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.springframework.lang.Nullable;
import ru.investbook.entity.SecurityEventCashFlowEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                .currency(cashFlow.getCurrency())
                .tax(tax.map(SecurityEventCashFlowEntity::getValue).map(BigDecimal::negate).orElse(null))
                .taxCurrency(tax.map(SecurityEventCashFlowEntity::getCurrency).orElse(null))
                .build();
    }

    Collection<SecurityEventCashFlow> getSecurityEventCashFlow(Map<Integer, String> accountToPortfolioId) {
        try {
            SecurityEventCashFlow cashFlow = SecurityEventCashFlow.builder()
                    .portfolio(Optional.of(accountToPortfolioId.get(account)).orElseThrow())
                    .security(asset)
                    .eventType(type.toCashFlowType())
                    .count(count.intValueExact())
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .value(amount)
                    .currency(currency)
                    .build();
            if (tax != null && Math.abs(tax.floatValue()) > 0.0001) {
                return Set.of(cashFlow,
                        cashFlow.toBuilder()
                                .eventType(CashFlowType.TAX)
                                .value(tax.negate())
                                .currency(taxCurrency)
                                .build());
            } else {
                return Set.of(cashFlow);
            }
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Collections.emptySet();
        }
    }
}
