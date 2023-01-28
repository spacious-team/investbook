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

package ru.investbook.web.forms.controller;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.repository.SecurityQuoteRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexIssSecurityQuoteService;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.SecurityQuoteModel;
import ru.investbook.web.forms.model.filter.SecurityQuoteFormFilterModel;
import ru.investbook.web.forms.service.SecurityQuoteFormsService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/security-quotes")
@Slf4j
@RequiredArgsConstructor
public class SecurityQuoteController {
    private final SecurityQuoteFormsService securityQuoteFormsService;
    private final MoexIssSecurityQuoteService moexIssSecurityQuoteService;
    private final SecurityRepository securityRepository;
    private final SecurityQuoteRepository securityQuoteRepository;
    private volatile Collection<String> securities;

    @PostConstruct
    public void start() {
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") SecurityQuoteFormFilterModel filter, Model model) {
        Page<SecurityQuoteModel> data = securityQuoteFormsService.getPage(filter);
        model.addAttribute("page", new PageableWrapperModel<>(data));

        return "security-quotes/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") SecurityQuoteFormFilterModel filter,
                         RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/security-quotes";
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
        String message = updateQuoteFromMoexIssApi();
        model.addAttribute("message", message);
        return "success";
    }

    public String updateQuoteFromMoexIssApi() throws InterruptedException, ExecutionException {
        long t0 = System.nanoTime();
        ForkJoinPool pool = new ForkJoinPool(4 * Runtime.getRuntime().availableProcessors());
        pool.submit(() -> securityRepository.findAll()
                        .parallelStream()
                        .forEach(moexIssSecurityQuoteService::updateQuote))
                .get();
        do {
            pool.shutdown();
        } while (!pool.awaitTermination(500, TimeUnit.MILLISECONDS));
        String message = securityQuoteRepository.findFirstByOrderByTimestampDesc()
                .map(SecurityQuoteEntity::getTimestamp)
                .map(instant -> LocalDate.ofInstant(instant, ZoneId.systemDefault()))
                .map(latestQuoteDate -> "Котировки обновлены по " + latestQuoteDate + " включительно")
                .orElse("Запрос выполнен, но МосБиржа не вернула котировок");
        log.info(message + " за " + Duration.ofNanos(System.nanoTime() - t0));
        return message;
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model) {
        securityQuoteFormsService.delete(id);
        model.addAttribute("message", "Котировка удалена");
        model.addAttribute("backLink", "/security-quotes");
        return "success";
    }
}
