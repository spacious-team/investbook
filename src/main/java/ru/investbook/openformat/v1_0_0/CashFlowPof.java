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
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.lang.Nullable;
import ru.investbook.entity.EventCashFlowEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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
                .currency(cashFlow.getCurrency())
                .type(PaymentTypePof.valueOf(CashFlowType.valueOf(cashFlow.getCashFlowType().getId())))
                .build();
    }
}
