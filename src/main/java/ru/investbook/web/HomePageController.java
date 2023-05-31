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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.InvestbookProperties;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomePageController {
    private final AssetsAndCashService assetsAndCashService;
    private final BuildProperties buildProperties;
    private final TransactionRepository transactionRepository;
    private final InvestbookProperties properties;

    @GetMapping
    public String index(Model model) {
        Set<String> portfolios = assetsAndCashService.getActivePortfolios();
        model.addAttribute("transactionsCount", transactionRepository.countByPortfolioIn(portfolios));
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("assets", assetsAndCashService.getTotalAssetsInRub(portfolios));
        model.addAttribute("cashBalance", assetsAndCashService.getTotalCashInRub(portfolios));
        model.addAttribute("buildProperties", buildProperties);
        model.addAttribute("logoUrl",
                "https://github.com/spacious-team/investbook/assets/11336712/97828ac2-c52f-4c6e-8c3a-8a16f2c3fa3a");
        if (properties.isTryAltIndexLogoUrl()) {
            model.addAttribute("altLogoUrl", "https://disk.yandex.ru/i/F7_F2K-eP7mnnQ");
        }
        return "index";
    }

    @GetMapping("shutdown")
    public String shutdown() {
        @SuppressWarnings("resource")
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> System.exit(0), 3, TimeUnit.SECONDS);
        return "shutdown-page";
    }
}
