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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.TransactionModel;
import ru.investbook.web.forms.service.TransactionFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionFormsService transactionFormsService;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private volatile List<String> securities;
    private volatile List<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(Model model) {
        model.addAttribute("transactions", transactionFormsService.getAll());
        return "transactions/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "portfolio", required = false) String portfolio,
                              @RequestParam(name = "transaction-id", required = false) String transactionId,
                              Model model) {
        model.addAttribute("transaction", getTransaction(portfolio, transactionId));
        model.addAttribute("securities", securities);
        model.addAttribute("portfolios", portfolios);
        return "transactions/edit-form";
    }

    private TransactionModel getTransaction(String portfolio, String transactionId) {
        if (portfolio != null && transactionId != null) {
            return transactionFormsService.getById(portfolio, transactionId)
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
     * @param transaction transaction attribute for save, same model attribute user for display in view
     */
    @PostMapping
    public String postTransaction(@Valid @ModelAttribute("transaction") TransactionModel transaction) {
        selectedPortfolio = transaction.getPortfolio();
        transactionFormsService.save(transaction);
        return "transactions/view-single";
    }
}
