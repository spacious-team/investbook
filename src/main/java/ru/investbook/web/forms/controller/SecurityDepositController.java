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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.SplitModel;
import ru.investbook.web.forms.model.TransactionModel;
import ru.investbook.web.forms.model.filter.TransactionFormFilterModel;
import ru.investbook.web.forms.service.TransactionFormsService;

@Controller
@RequestMapping("/security-deposit")
public class SecurityDepositController extends TransactionController {

    public SecurityDepositController(TransactionFormsService transactionFormsService,
                                     PortfolioRepository portfolioRepository, SecurityRepository securityRepository,
                                     FifoPositionsFactory fifoPositionsFactory) {
        super(transactionFormsService, portfolioRepository, securityRepository, fifoPositionsFactory);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") TransactionFormFilterModel filter, Model model) {
        Page<TransactionModel> page = transactionFormsService.getSecurityDepositPage(filter);
        portfolios = ControllerHelper.getPortfolios(portfolioRepository); // update portfolios for filter
        model.addAttribute("page", new PageableWrapperModel<>(page));
        model.addAttribute("portfolios", portfolios);

        return "security-deposit/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") TransactionFormFilterModel filter, RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/security-deposit";
    }

    @GetMapping("/edit-form")
    @Override
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        super.getEditForm(id, model);
        return "security-deposit/edit-form";
    }

    @GetMapping("/create-split-form")
    public String getCreateSplitForm(Model model) {
        model.addAttribute("split", new SplitModel());
        model.addAttribute("securities", securities);
        model.addAttribute("portfolios", portfolios);
        return "security-deposit/create-split-form";
    }

    /**
     * Saves transaction to storage
     *
     * @param transaction transaction attribute for save, same model attribute used for display in view
     */
    @PostMapping
    @Override
    public String postTransaction(@ModelAttribute("transaction") @Valid TransactionModel transaction) {
        super.postTransaction(transaction);
        return "security-deposit/view-single";
    }

    @PostMapping("split")
    public String postSplit(@ModelAttribute("split") @Valid SplitModel splitModel) {
        selectedPortfolio = splitModel.getPortfolio();
        transactionFormsService.save(splitModel);
        fifoPositionsFactory.invalidateCache();
        return "security-deposit/view-split";
    }

    @GetMapping("/delete")
    @Override
    public String delete(@RequestParam(name = "id") int id, Model model) {
        super.doDelete(id);
        model.addAttribute("message", "Запись удалена");
        model.addAttribute("backLink", "/security-deposit");
        return "success";
    }
}
