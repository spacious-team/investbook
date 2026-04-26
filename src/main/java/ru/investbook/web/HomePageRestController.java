/*
 * InvestBook
 * Copyright (C) 2026  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

@RestController
@RequiredArgsConstructor
public class HomePageRestController {
    private final AssetsAndCashService assetsAndCashService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/accounts/all/stats")
    @Tag(name = "Accounts")
    @Operation(summary = "Get statistics for all accounts", operationId = "accountsAllStats", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public AccountStats portfolioStats() {
        Set<String> accounts = assetsAndCashService.getActiveAccounts();
        return AccountStats.builder()
                .accounts(accounts)
                .totalTransactions(transactionRepository.countByAccountIn(accounts))
                .assetsValue(assetsAndCashService.getTotalAssetsInRub(accounts).orElse(null))
                .cashBalance(assetsAndCashService.getTotalCashInRub(accounts).orElse(null))
                .build();
    }

    @PostMapping("/app/shutdown")
    @Tag(name = "Application")
    @Operation(summary = "Shutdown app", operationId = "appShutdown", responses = {
            @ApiResponse(responseCode = "202"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> shutdown() {
        @SuppressWarnings("resource")
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> System.exit(0), 3, SECONDS);
        return ResponseEntity.accepted().build();
    }


    @Data
    @Builder
    @Jacksonized
    @Schema(name = "AccountStats")
    public static class AccountStats {

        private final Set<String> accounts;

        @JsonProperty("total-transactions")
        private final int totalTransactions;

        @JsonProperty("assets-value")
        private final @Nullable  BigDecimal assetsValue;

        @JsonProperty("cash-balance")
        private final @Nullable BigDecimal cashBalance;
    }
}
