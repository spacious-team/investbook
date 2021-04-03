/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.web.forms.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexIssSecurityQuoteService;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.service.SecurityQuoteFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

@Controller
@RequestMapping("/security-quotes")
@Slf4j
@RequiredArgsConstructor
public class SecurityQuoteController {
    private final SecurityQuoteFormsService securityQuoteFormsService;
    private final MoexIssSecurityQuoteService moexIssSecurityQuoteService;
    private final SecurityRepository securityRepository;
    private volatile Collection<String> securities;

    @PostConstruct
    public void start() {
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(Model model) {
        model.addAttribute("quotes", securityQuoteFormsService.getAll());
        return "security-quotes/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("quote", getSecurityQuote(id));
        model.addAttribute("securities", securities);
        return "security-quotes/edit-form";
    }

    private SecurityQuoteModel getSecurityQuote(Integer id) {
        if (id != null) {
            return securityQuoteFormsService.getById(id)
                    .orElseGet(SecurityQuoteModel::new);
        } else {
            return new SecurityQuoteModel();
        }
    }

    @PostMapping
    public String postSecurityQuote(@Valid  @ModelAttribute("quote") SecurityQuoteModel quote) {
        securityQuoteFormsService.save(quote);
        return "security-quotes/view-single";
    }

    @GetMapping("update")
    public String updateFromMoexIssApi(Model model) throws ExecutionException, InterruptedException {
        long t0 = System.nanoTime();
        new ForkJoinPool(4 * Runtime.getRuntime().availableProcessors())
                .submit(() -> securityRepository.findAll()
                        .parallelStream()
                        .forEach(moexIssSecurityQuoteService::updateQuote))
                .get();
        log.info("Котировки обновлены за {}", Duration.ofNanos(System.nanoTime() - t0));
        model.addAttribute("message", "Котировки обновлены.");
        return "success";
    }
}
