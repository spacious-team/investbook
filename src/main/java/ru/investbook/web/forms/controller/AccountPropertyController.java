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
import ru.investbook.web.forms.model.AccountPropertyModel;
import ru.investbook.web.forms.model.AccountPropertyTotalAssetsModel;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.filter.AccountPropertyFormFilterModel;
import ru.investbook.web.forms.service.AccountPropertyFormsService;

import java.util.Collection;
import java.util.function.Supplier;

@Controller
@RequestMapping("/account-properties")
@RequiredArgsConstructor
public class AccountPropertyController {
    private final AccountPropertyFormsService accountPropertyFormsService;
    private final AccountRepository accountRepository;
    private volatile Collection<String> accounts;
    private volatile String selectedAccount;

    @PostConstruct
    public void start() {
        accounts = ControllerHelper.getAccounts(accountRepository);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") AccountPropertyFormFilterModel filter, Model model) {
        Page<AccountPropertyModel> page = accountPropertyFormsService.getPage(filter);
        accounts = ControllerHelper.getAccounts(accountRepository); // update accounts for filter
        model.addAttribute("page", new PageableWrapperModel<>(page));
        model.addAttribute("accounts", accounts);
        return "account-properties/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") AccountPropertyFormFilterModel filter,
                         RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/account-properties";
    }

    @GetMapping("/edit-form/total-assets")
    public String getTotalAssetsEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        AccountPropertyTotalAssetsModel property = (AccountPropertyTotalAssetsModel)
                getAccountProperty(id, AccountPropertyTotalAssetsModel::new);
        model.addAttribute("property", property);
        model.addAttribute("accounts", accounts);
        return "account-properties/total-assets-edit-form";
    }

    private AccountPropertyModel getAccountProperty(Integer id,
                                                    Supplier<? extends AccountPropertyModel> newSupplier) {
        if (id != null) {
            return accountPropertyFormsService.getById(id)
                    .orElseGet(newSupplier);
        } else {
            AccountPropertyModel model = newSupplier.get();
            model.setAccount(selectedAccount);
            return model;
        }
    }

    @PostMapping("/total-assets")
    public String postCash(@ModelAttribute("property") @Valid AccountPropertyTotalAssetsModel property) {
        selectedAccount = property.getAccount();
        accountPropertyFormsService.save(property);
        return "account-properties/total-assets-view-single";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model) {
        accountPropertyFormsService.delete(id);
        model.addAttribute("message", "Запись удалена");
        model.addAttribute("backLink", "/account-properties");
        return "success";
    }
}
