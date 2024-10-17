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
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import ru.investbook.entity.TransactionEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.spacious_team.broker.pojo.CashFlowType.FEE;
import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder(toBuilder = true)
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class TransferPof {
    @JsonProperty("id")
    @NotNull
    int id;

    /**
     * Если описывается сплит акций, то может иметь одинаковое значение для нескольких объектов, которое разрешается
     * {@link PortfolioOpenFormatPersister}
     */
    @JsonProperty("transfer-id")
    @Nullable
    String transferId;

    @JsonProperty("account")
    @NotNull
    int account;

    @JsonProperty("timestamp")
    @NotNull
    long timestamp;

    @JsonProperty("asset")
    @Getter(AccessLevel.NONE)
    @NotNull
    int asset;

    /**
     * Поддерживаются дробные акции
     */
    @JsonProperty("count")
    @NotNull
    BigDecimal count;

    @JsonProperty("fee-account")
    @Nullable
    Integer feeAccount;

    @JsonProperty("fee")
    @Nullable
    BigDecimal fee;

    @JsonProperty("fee-currency")
    @Nullable
    String feeCurrency;

    @JsonProperty("description")
    @Nullable
    String description;

    static TransferPof of(TransactionEntity transaction) {
        return TransferPof.builder()
                .id(transaction.getId())
                .transferId(transaction.getTradeId())
                .account(AccountPof.getAccountId(transaction.getPortfolio()))
                .timestamp(transaction.getTimestamp().getEpochSecond())
                .asset(transaction.getSecurity().getId())
                .count(BigDecimal.valueOf(transaction.getCount()))
                .build();
    }

    Optional<Transaction> toTransaction(Map<Integer, String> accountToPortfolioId,
                                        Map<Integer, Integer> assetToSecurityId) {
        try {
            return Optional.of(Transaction.builder()
                    .tradeId(transferId)
                    .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .security(getSecurityId(assetToSecurityId))
                    .count(count.intValueExact())
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    Collection<SecurityEventCashFlow> getSecurityEventCashFlow(Map<Integer, String> accountToPortfolioId,
                                                               Map<Integer, Integer> assetToSecurityId) {
        try {
            if (fee != null) {
                return Set.of(
                        SecurityEventCashFlow.builder()
                                .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                                .timestamp(Instant.ofEpochSecond(timestamp))
                                .security(getSecurityId(assetToSecurityId))
                                .count(count.intValueExact())
                                .eventType(FEE)
                                .value(fee.negate())
                                .currency(getValidCurrencyOrNull(feeCurrency))
                                .build());
            } else {
                return Collections.emptySet();
            }
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Collections.emptyList();
        }
    }

    int getSecurityId(Map<Integer, Integer> assetToSecurityId) {
        return Objects.requireNonNull(assetToSecurityId.get(asset));
    }
}
