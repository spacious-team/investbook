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
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.lang.Nullable;
import ru.investbook.entity.PortfolioEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class AccountPof {

    private static final ThreadLocal<AtomicInteger> idGenerator = ThreadLocal.withInitial(AtomicInteger::new);
    private static final ThreadLocal<Map<String, Integer>> accountNumberToIdMap = ThreadLocal.withInitial(HashMap::new);

    @NotNull
    @JsonProperty("id")
    int id;

    @Nullable
    @JsonProperty("account-number")
    String accountNumber;

    @NotNull
    @JsonProperty("type")
    AccountTypePof type;

    @NotNull
    @JsonProperty("valuation")
    BigDecimal valuation;

    @NotEmpty
    @JsonProperty("valuation-currency")
    String valuationCurrency;

    static void resetAccountIdGenerator() {
        idGenerator.get().set(0);
        accountNumberToIdMap.get().clear();
    }

    static int getAccountId(String accountNumber) {
        return Objects.requireNonNull(accountNumberToIdMap.get().get(accountNumber));
    }

    static AccountPof of(PortfolioEntity portfolio, BigDecimal valuationInRub) {
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

    Optional<Portfolio> toPortfolio() {
        try {
            String portfolioId = Optional.ofNullable(accountNumber)
                    .orElse(String.valueOf(id));
            return Optional.of(Portfolio.builder()
                    .id(portfolioId)
                    .build());
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Optional.empty();
        }
    }
}
