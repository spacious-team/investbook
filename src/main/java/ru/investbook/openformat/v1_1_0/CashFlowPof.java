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
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.springframework.lang.Nullable;
import ru.investbook.entity.EventCashFlowEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class CashFlowPof {

    @NotNull
    @JsonProperty("id")
    int id;

    @Nullable
    @JsonProperty("flow-id")
    String flowId;

    @NotNull
    @JsonProperty("account")
    int account;

    @NotNull
    @JsonProperty("timestamp")
    long timestamp;

    @NotNull
    @JsonProperty("amount")
    BigDecimal amount;

    @NotEmpty
    @JsonProperty("currency")
    String currency;

    @NotNull
    @JsonProperty("type")
    PaymentTypePof type;

    @Nullable
    @JsonProperty("description")
    String description;

    static CashFlowPof of(EventCashFlowEntity cashFlow) {
        return CashFlowPof.builder()
                .id(cashFlow.getId())
                .account(AccountPof.getAccountId(cashFlow.getPortfolio().getId()))
                .timestamp(cashFlow.getTimestamp().getEpochSecond())
                .amount(cashFlow.getValue())
                .currency(getValidCurrencyOrNull(cashFlow.getCurrency()))
                .type(PaymentTypePof.valueOf(CashFlowType.valueOf(cashFlow.getCashFlowType().getId())))
                .description(cashFlow.getDescription())
                .build();
    }

    Optional<EventCashFlow> toEventCashFlow(Map<Integer, String> accountToPortfolioId) {
        try {
            CashFlowType eventType = type.toCashFlowType();
            if (eventType == CashFlowType.DIVIDEND && description != null && description.contains("купон")) {
                eventType = CashFlowType.COUPON; // izi-invest.ru fix: не различает дивиденды и купоны
            }
            return Optional.of(EventCashFlow.builder()
                    .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .value(amount)
                    .currency(getValidCurrencyOrNull(currency))
                    .eventType(eventType)
                    .description(description)
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }
}
