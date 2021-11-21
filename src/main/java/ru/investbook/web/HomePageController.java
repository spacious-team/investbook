/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.springframework.web.bind.annotation.ResponseBody;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class HomePageController {

    private final AssetsAndCashService assetsAndCashService;
    private final BuildProperties buildProperties;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public String index(Model model) {
        Set<String> portfolios = assetsAndCashService.getActivePortfolios();
        model.addAttribute("transactionsCount", transactionRepository.countByPortfolioIn(portfolios));
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("assets", assetsAndCashService.getAssets(portfolios));
        model.addAttribute("cashBalance", assetsAndCashService.getTotalCash(portfolios));
        model.addAttribute("buildProperties", buildProperties);
        return "index";
    }

    @GetMapping("shutdown")
    @ResponseBody
    public String shutdown() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(0), 3, TimeUnit.SECONDS);
        return "Приложение остановлено";
    }
}
