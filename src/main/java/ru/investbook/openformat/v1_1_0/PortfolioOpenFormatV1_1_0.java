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
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

import static java.util.Collections.emptySet;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioOpenFormatV1_1_0 {
    public static String GENERATED_BY_INVESTBOOK = "investbook";

    @Builder.Default
    @JsonProperty("version")
    @NotNull
    String version = "1.1.0";

    @Builder.Default
    @JsonProperty("generated-by")
    @NotNull
    String generatedBy = GENERATED_BY_INVESTBOOK;

    @Builder.Default
    @JsonProperty("generated")
    long generated = System.currentTimeMillis() / 1000;

    @JsonProperty("end")
    long end;

    @JsonProperty("start")
    @Nullable
    Long start;

    @Builder.Default
    @JsonProperty("accounts")
    @NotNull
    Collection<AccountPof> accounts = emptySet();

    @Builder.Default
    @JsonProperty("cash-balances")
    @NotNull
    Collection<CashBalancesPof> cashBalances = emptySet();

    @Builder.Default
    @JsonProperty("assets")
    @NotNull
    Collection<AssetPof> assets = emptySet();

    @Builder.Default
    @JsonProperty("trades")
    @NotNull
    Collection<TradePof> trades = emptySet();

    @Builder.Default
    @JsonProperty("transfers")
    @NotNull
    Collection<TransferPof> transfer = emptySet();

    @Builder.Default
    @JsonProperty("payments")
    @NotNull
    Collection<PaymentPof> payments = emptySet();

    @Builder.Default
    @JsonProperty("cash-flows")
    @NotNull
    Collection<CashFlowPof> cashFlows = emptySet();

    @JsonProperty("vnd-investbook")
    @Nullable
    VndInvestbookPof vndInvestbook;
}
