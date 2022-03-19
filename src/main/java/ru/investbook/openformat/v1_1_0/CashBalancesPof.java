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
import org.spacious_team.broker.pojo.PortfolioCash;
import ru.investbook.entity.PortfolioCashEntity;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.investbook.openformat.OpenFormatHelper.getValidCurrencyOrNull;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class CashBalancesPof {

    @NotNull
    @JsonProperty("account")
    int account;

    @NotNull
    @Builder.Default
    @JsonProperty("cash")
    Collection<CashPof> cash = Collections.emptyList();

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class CashPof {
        @NotNull
        @JsonProperty("value")
        BigDecimal value;

        @NotEmpty
        @JsonProperty("currency")
        String currency;
    }

    static CashBalancesPof of(int account, Collection<PortfolioCashEntity> cashBalances) {
        return CashBalancesPof.builder()
                .account(account)
                .cash(getCashBalances(cashBalances))
                .build();
    }

    private static Collection<CashPof> getCashBalances(Collection<PortfolioCashEntity> cashBalances) {
        Map<String, BigDecimal> sumByCurrency = cashBalances.stream()
                .collect(Collectors.toMap(
                        PortfolioCashEntity::getCurrency,
                        PortfolioCashEntity::getValue,
                        BigDecimal::add));
        return sumByCurrency.entrySet()
                .stream()
                .filter(e -> Math.abs(e.getValue().floatValue()) > 1e-4)
                .map(e -> new CashPof(e.getValue(), getValidCurrencyOrNull(e.getKey())))
                .toList();
    }

    Collection<PortfolioCash> toPortfolioCash(Map<Integer, String> accountToPortfolioId, Instant instant) {
        try {
            return cash.stream()
                    .map(c -> PortfolioCash.builder()
                            .portfolio(Objects.requireNonNull(accountToPortfolioId.get(account)))
                            .market("all")
                            .timestamp(instant)
                            .value(c.getValue())
                            .currency(c.getCurrency())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Не могу распарсить {}", this, e);
            return Collections.emptySet();
        }
    }
}
