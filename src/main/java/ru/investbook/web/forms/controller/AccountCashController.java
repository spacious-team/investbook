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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.investbook.repository.AccountRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.AccountCashModel;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.filter.AccountCashFormFilterModel;
import ru.investbook.web.forms.service.AccountCashFormsService;

import java.util.Collection;
import java.util.function.Supplier;

@Controller
@RequestMapping("/account-cash")
@RequiredArgsConstructor
public class AccountCashController {
    private final AccountCashFormsService accountCashFormsService;
    private final AccountRepository accountRepository;
    private volatile Collection<String> accounts;
    private volatile String selectedAccount;

    @PostConstruct
    public void start() {
        accounts = ControllerHelper.getAccounts(accountRepository);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") AccountCashFormFilterModel filter, Model model) {
        Page<AccountCashModel> data = accountCashFormsService.getPage(filter);
        accounts = ControllerHelper.getAccounts(accountRepository); // update accounts for filter
        model.addAttribute("page", new PageableWrapperModel<>(data));
        model.addAttribute("accounts", accounts);

        return "account-cash/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") AccountCashFormFilterModel filter,
                         RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/account-cash";
    }

    @GetMapping("/edit-form")
    public String getCashEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        AccountCashModel cash = getAccountCash(id, AccountCashModel::new);
        model.addAttribute("cash", cash);
        model.addAttribute("accounts", accounts);
        return "account-cash/edit-form";

    }

    private AccountCashModel getAccountCash(Integer id, Supplier<? extends AccountCashModel> newSupplier) {
        if (id != null) {
            return accountCashFormsService.getById(id)
                    .orElseGet(newSupplier);
        } else {
            AccountCashModel model = newSupplier.get();
            model.setAccount(selectedAccount);
            return model;
        }
    }

    @PostMapping
    public String postCash(@ModelAttribute("cash") @Valid AccountCashModel cash) {
        selectedAccount = cash.getAccount();
        accountCashFormsService.save(cash);
        return "account-cash/view-single";
    }


    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model) {
        accountCashFormsService.delete(id);
        model.addAttribute("message", "Запись удалена");
        model.addAttribute("backLink", "/account-cash");
        return "success";
    }
}
