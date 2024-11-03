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
import java.util.Collections;

@Jacksonized
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioOpenFormatV1_1_0 {
    public static String GENERATED_BY_INVESTBOOK = "investbook";

    @NotNull
    @Builder.Default
    @JsonProperty("version")
    String version = "1.1.0";

    @NotNull
    @Builder.Default
    @JsonProperty("generated-by")
    String generatedBy = GENERATED_BY_INVESTBOOK;

    @NotNull
    @Builder.Default
    @JsonProperty("generated")
    long generated = System.currentTimeMillis() / 1000;

    @NotNull
    @JsonProperty("end")
    long end;

    @Nullable
    @JsonProperty("start")
    Long start;

    @NotNull
    @Builder.Default
    @JsonProperty("accounts")
    Collection<AccountPof> accounts = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("cash-balances")
    Collection<CashBalancesPof> cashBalances = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("assets")
    Collection<AssetPof> assets = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("trades")
    Collection<TradePof> trades = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("transfers")
    Collection<TransferPof> transfer = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("payments")
    Collection<PaymentPof> payments = Collections.emptySet();

    @NotNull
    @Builder.Default
    @JsonProperty("cash-flows")
    Collection<CashFlowPof> cashFlows = Collections.emptySet();

    @Nullable
    @JsonProperty("vnd-investbook")
    VndInvestbookPof vndInvestbook;
}
