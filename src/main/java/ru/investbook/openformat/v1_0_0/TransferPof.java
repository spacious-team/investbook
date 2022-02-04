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
import ru.investbook.entity.TransactionEntity;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferPof {
    @NotNull
    @JsonProperty("id")
    int id;

    @Nullable
    @JsonProperty("transfer-id")
    String transferId;

    /**
     * Если значение null, то значение поля settlement обязательно
     */
    @NotNull
    @JsonProperty("timestamp")
    long timestamp;

    @Nullable
    @JsonProperty("withdrawal-account")
    Integer withdrawalAccount;

    @Nullable
    @JsonProperty("withdrawal-asset")
    Integer withdrawalAsset;

    /**
     * Поддерживаются дробные акции
     */
    @Nullable
    @JsonProperty("withdrawal")
    BigDecimal withdrawal;

    @Nullable
    @JsonProperty("deposit-account")
    Integer depositAccount;

    @Nullable
    @JsonProperty("deposit-asset")
    Integer depositAsset;

    /**
     * Поддерживаются дробные акции
     */
    @Nullable
    @JsonProperty("deposit")
    BigDecimal deposit;

    @Nullable
    @JsonProperty("fee-account")
    Integer feeAccount;

    @Nullable
    @JsonProperty("fee")
    BigDecimal fee;

    @Nullable
    @JsonProperty("fee-currency")
    String feeCurrency;

    @Nullable
    @JsonProperty("description")
    String description;

    static TransferPof of(TransactionEntity transaction) {
        int count = transaction.getCount();
        TransferPof.TransferPofBuilder builder = TransferPof.builder()
                .id(transaction.getId())
                .transferId(transaction.getTradeId())
                .timestamp(transaction.getTimestamp().getEpochSecond());
        if (count >= 0) {
            builder
                    .depositAccount(AccountPof.getAccountId(transaction.getPortfolio()))
                    .depositAsset(transaction.getSecurity().getId())
                    .deposit(BigDecimal.valueOf(count));
        } else {
            builder
                    .withdrawalAccount(AccountPof.getAccountId(transaction.getPortfolio()))
                    .withdrawalAsset(transaction.getSecurity().getId())
                    .withdrawal(BigDecimal.valueOf(count));
        }
        return builder.build();
    }
}
