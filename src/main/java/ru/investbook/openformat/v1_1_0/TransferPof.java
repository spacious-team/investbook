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
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.Transaction;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import ru.investbook.entity.TransactionEntity;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.spacious_team.broker.pojo.CashFlowType.COMMISSION;
import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class TransferPof {
    @NotNull
    @JsonProperty("id")
    int id;

    /**
     * Если описывается сплит акций, то может иметь одинаковое значение для нескольких объектов.
     */
    @Nullable
    @JsonProperty("transfer-id")
    String transferId;

    @NotNull
    @JsonProperty("account")
    int account;

    /**
     * Если значение null, то значение поля settlement обязательно
     */
    @NotNull
    @JsonProperty("timestamp")
    long timestamp;

    @NotNull
    @JsonProperty("asset")
    int asset;

    /**
     * Поддерживаются дробные акции
     */
    @NotNull
    @JsonProperty("count")
    BigDecimal count;

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
        return TransferPof.builder()
                .id(transaction.getId())
                .transferId(transaction.getTradeId())
                .account(AccountPof.getAccountId(transaction.getPortfolio()))
                .timestamp(transaction.getTimestamp().getEpochSecond())
                .asset(transaction.getSecurity().getId())
                .count(BigDecimal.valueOf(transaction.getCount()))
                .build();
    }

    Optional<Transaction> toTransaction(Map<Integer, String> accountToPortfolioId) {
        try {
            return Optional.of(Transaction.builder()
                    .tradeId(getTradeId())
                    .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                    .timestamp(Instant.ofEpochSecond(timestamp))
                    .security(asset)
                    .count(count.intValueExact())
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    private String getTradeId() {
        String tradeId;
        if (StringUtils.hasLength(transferId)) {
            tradeId = transferId.endsWith(String.valueOf(account)) ? transferId : transferId + account;
        } else {
            tradeId = timestamp + ":" + asset + ":" + account;
        }
        return tradeId;
    }

    Collection<SecurityEventCashFlow> getSecurityEventCashFlow(Map<Integer, String> accountToPortfolioId) {
        try {
            if (fee != null) {
                return Set.of(
                        SecurityEventCashFlow.builder()
                                .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                                .timestamp(Instant.ofEpochSecond(timestamp))
                                .security(asset)
                                .count(count.intValueExact())
                                .eventType(COMMISSION)
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
}
