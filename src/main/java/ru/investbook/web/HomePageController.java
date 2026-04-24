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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.InvestbookProperties;
import ru.investbook.parser.BrokerReportParserService;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.AssetsAndCashService;
import ru.investbook.web.forms.controller.ExternalSourcesGetterController;

import java.io.InputStream;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class HomePageController {
    private final AssetsAndCashService assetsAndCashService;
    private final BuildProperties buildProperties;
    private final TransactionRepository transactionRepository;
    private final InvestbookProperties properties;
    private final BrokerReportParserService brokerReportParserService;
    private final ExternalSourcesGetterController externalSourcesGetterController;

    @GetMapping
    public String index(Model model) {
        Set<String> accounts = assetsAndCashService.getActiveAccounts();
        model.addAttribute("transactionsCount", transactionRepository.countByAccountIn(accounts));
        model.addAttribute("accounts", accounts);
        model.addAttribute("assets", assetsAndCashService.getTotalAssetsInRub(accounts));
        model.addAttribute("cashBalance", assetsAndCashService.getTotalCashInRub(accounts));
        model.addAttribute("buildProperties", buildProperties);
        model.addAttribute("logoUrl",
                "https://github.com/spacious-team/investbook/assets/11336712/97828ac2-c52f-4c6e-8c3a-8a16f2c3fa3a");
        if (properties.isTryAltIndexLogoUrl()) {
            model.addAttribute("altLogoUrl", "https://disk.yandex.ru/i/F7_F2K-eP7mnnQ");
        }
        return "index";
    }

    @PostMapping("/portfolios/demo/upload")
    public String uploadDemo(Model model) {
        String file = "/static/demo-portfolio.xlsx";
        try (InputStream inputStream = requireNonNull(getClass().getResourceAsStream(file))) {
            brokerReportParserService.parseReport(inputStream, file);
            return externalSourcesGetterController.updateSilently(model, "/");
        } catch (Exception e) {
            log.error("Can't upload demo portfolio", e);
            model.addAttribute("title", "Ошибка загрузки");
            model.addAttribute("message", "Заведите заявку об ошибке на GitHub. " +
                    "Диагностическая информация: " + e.getMessage());
            model.addAttribute("backLink", "/");
            return "success";
        }
    }

    @GetMapping("/app/shutdown")
    public String shutdown() {
        return "app/shutdown";
    }
}
