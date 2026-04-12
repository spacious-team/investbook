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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.AccountRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.forms.model.ArchivedAccountModel;

import java.util.Set;

import static ru.investbook.web.ControllerHelper.getAccounts;
import static ru.investbook.web.ControllerHelper.getInactiveAccounts;

@Controller
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountRepository accountRepository;
    private final SecurityRepository securityRepository;
    private final FifoPositionsFactory fifoPositionsFactory;
    private final ForeignExchangeRateService foreignExchangeRateService;

    @GetMapping("/archive")
    public String get(Model model, @ModelAttribute("archive") ArchivedAccountModel archive) {
        Set<String> allAccounts = getAccounts(accountRepository);
        model.addAttribute("allAccounts", allAccounts);
        archive.setAccounts(getInactiveAccounts(accountRepository));
        return "accounts/archive";
    }

    @PostMapping("/archive")
    public String postEventCashFlow(@ModelAttribute("archive") @Valid ArchivedAccountModel archive) {
        getAccounts(accountRepository)
                .forEach(account -> {
                    boolean isEnabled = !archive.getAccounts().contains(account);
                    accountRepository.setEnabledForAccount(account, isEnabled);
                });
        return "success";
    }

    @GetMapping("/delete-all")
    public String deleteAllWarning(Model model) {
        model.addAttribute("title", "Внимание!");
        model.addAttribute("message", """
                Вы пытаетесь удалить все данные (сделки, выплаты, движения денежных средств и т.д.) для всех счетов.
                Эта операция не обратима. Если вы все же настаиваете на удалении, то рекомендуем прежде скачать
                <a href="/portfolio-open-format/download">бэкап</a> данных.
                <br><br>
                Для подтверждения удаления всех данных нажмите на ссылку
                <a href="/accounts/delete-all-accepted">[подтверждаю]</a>.
                """);
        return "success";
    }

    @GetMapping("/delete-all-accepted")
    public String deleteAllAccepted(Model model) {
        accountRepository.deleteAll();
        securityRepository.deleteAll();
        fifoPositionsFactory.invalidateCache();
        foreignExchangeRateService.invalidateCache();
        model.addAttribute("message", "Информация по всем счетам удалена");
        model.addAttribute("backLink", "/forms.html");
        return "success";
    }
}
