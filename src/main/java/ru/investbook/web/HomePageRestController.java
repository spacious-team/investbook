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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

@RestController
@Tag(name = "Приложение")
@RequiredArgsConstructor
public class HomePageRestController {
    private final AssetsAndCashService assetsAndCashService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/portfolios/all/stats")
    @Operation(summary = "Статистика портфеля", operationId = "portfoliosAllStats", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public Map<String, Object> portfolioStats() {
        Set<String> accounts = assetsAndCashService.getActiveAccounts();
        return Map.of(
                "accounts", accounts,
                "total-transactions", transactionRepository.countByAccountIn(accounts),
                "assets-value", assetsAndCashService.getTotalAssetsInRub(accounts),
                "cash-balance", assetsAndCashService.getTotalCashInRub(accounts));
    }

    @PostMapping("/app/shutdown")
    @GetMapping("/app/shutdown")
    @Operation(summary = "Закрыть приложение", operationId = "appShutdown", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public Map<String, String> shutdown() {
        @SuppressWarnings("resource")
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> System.exit(0), 3, SECONDS);
        return Map.of("status", "Shutdown scheduled in 3 seconds");
    }
}
