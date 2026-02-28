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
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Account;
import org.spacious_team.broker.pojo.AccountProperty;
import org.spacious_team.broker.pojo.AccountProperty.AccountPropertyBuilder;
import org.spacious_team.broker.pojo.AccountPropertyType;
import ru.investbook.entity.AccountEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.spacious_team.broker.pojo.AccountPropertyType.TOTAL_ASSETS_RUB;
import static org.spacious_team.broker.pojo.AccountPropertyType.TOTAL_ASSETS_USD;
import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AccountPof {

    @SuppressWarnings("type.argument")
    private static final ThreadLocal<AtomicInteger> idGenerator = ThreadLocal.withInitial(AtomicInteger::new);
    @SuppressWarnings("type.argument")
    private static final ThreadLocal<Map<String, Integer>> accountNumberToIdMap = ThreadLocal.withInitial(HashMap::new);

    @JsonProperty("id")
    int id;


    @JsonProperty("account-number")
    @Nullable
    String accountNumber;

    @JsonProperty("type")
    @NotNull
    AccountTypePof type;

    @JsonProperty("valuation")
    @NotNull
    BigDecimal valuation;

    @JsonProperty("valuation-currency")
    @NotEmpty
    String valuationCurrency;

    static void resetAccountIdGenerator() {
        idGenerator.get().set(0);
        accountNumberToIdMap.get().clear();
    }

    static int getAccountId(String accountNumber) {
        return Objects.requireNonNull(accountNumberToIdMap.get().get(accountNumber));
    }

    static AccountPof of(AccountEntity portfolio, BigDecimal valuationInRub) {
        int id = idGenerator.get().incrementAndGet();
        accountNumberToIdMap.get().put(portfolio.getId(), id);
        return AccountPof.builder()
                .id(id)
                .accountNumber(portfolio.getId())
                .type(AccountTypePof.investment)
                .valuation(valuationInRub)
                .valuationCurrency("RUB")
                .build();
    }

    Optional<Account> toAccount() {
        try {
            String account = Optional.ofNullable(accountNumber)
                    .orElse(String.valueOf(id));
            return Optional.of(Account.builder()
                    .id(account)
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    Optional<AccountProperty> toTotalAssets(Instant atInstant) {
        try {
            return getAccountPropertyType()
                    .map(this::getPortfolioProperty)
                    .map(p -> p.timestamp(atInstant))
                    .map(AccountPropertyBuilder::build);
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }

    private Optional<AccountPropertyType> getAccountPropertyType() {
        String currency = getValidCurrencyOrNull(valuationCurrency);
        if ("RUB".equalsIgnoreCase(currency)) {
            return Optional.of(TOTAL_ASSETS_RUB);
        } else if ("USD".equalsIgnoreCase(currency)) {
            return Optional.of(TOTAL_ASSETS_USD);
        }
        return Optional.empty();
    }

    private AccountPropertyBuilder getPortfolioProperty(AccountPropertyType type) {
        String account = Optional.ofNullable(accountNumber)
                .orElse(String.valueOf(id));
        return AccountProperty.builder()
                .account(account)
                .property(type)
                .value(valuation.toString());
    }
}
