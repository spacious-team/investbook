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

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class HomePageRestController {
    private final AssetsAndCashService assetsAndCashService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/portfolio-stats")
    public Map<String, Object> portfolioStats() {
        Set<String> accounts = assetsAndCashService.getActiveAccounts();
        return Map.of(
                "accounts", accounts,
                "total-transactions", transactionRepository.countByAccountIn(accounts),
                "assets-value", assetsAndCashService.getTotalAssetsInRub(accounts),
                "cash-balance", assetsAndCashService.getTotalCashInRub(accounts));
    }
}
