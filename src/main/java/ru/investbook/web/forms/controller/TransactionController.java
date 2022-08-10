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

import lombok.RequiredArgsConstructor;
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
import ru.investbook.web.forms.model.TransactionModel;
import ru.investbook.web.forms.model.filter.TransactionFormFilterModel;
import ru.investbook.web.forms.service.TransactionFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.Collection;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    protected final TransactionFormsService transactionFormsService;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    protected final FifoPositionsFactory fifoPositionsFactory;
    protected volatile Collection<String> securities;
    protected volatile Collection<String> portfolios;
    protected volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") TransactionFormFilterModel filter, Model model) {
        var data = transactionFormsService.getPage(filter);
        model.addAttribute("page", new PageableWrapperModel<>(data));
        model.addAttribute("portfolios", portfolios);

        return "transactions/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") TransactionFormFilterModel filter, RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/transactions";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("transaction", getTransaction(id));
        model.addAttribute("securities", securities);
        model.addAttribute("portfolios", portfolios);
        return "transactions/edit-form";
    }

    private TransactionModel getTransaction(Integer id) {
        if (id != null) {
            return transactionFormsService.getById(id)
                    .orElseGet(TransactionModel::new);
        } else {
            TransactionModel transaction = new TransactionModel();
            transaction.setPortfolio(selectedPortfolio);
            return transaction;
        }
    }

    /**
     * Saves transaction to storage
     *
     * @param transaction transaction attribute for save, same model attribute used for display in view
     */
    @PostMapping
    public String postTransaction(@Valid @ModelAttribute("transaction") TransactionModel transaction) {
        selectedPortfolio = transaction.getPortfolio();
        transactionFormsService.save(transaction);
        fifoPositionsFactory.invalidateCache();
        return "transactions/view-single";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") int id, Model model) {
        doDelete(id);
        model.addAttribute("message", "Сделка удалена");
        model.addAttribute("backLink", "/transactions");
        return "success";
    }

    protected void doDelete(int id) {
        transactionFormsService.delete(id);
        fifoPositionsFactory.invalidateCache();
    }
}
