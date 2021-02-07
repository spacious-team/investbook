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

package ru.investbook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.model.dto.TransactionModel;
import ru.investbook.model.repository.TransactionModelRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Controller
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionModelRepository transactionModelRepository;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private volatile List<String> securities;
    private volatile List<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = portfolioRepository.findAll()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toList());
        securities = securityRepository.findAll()
                .stream()
                .map(e -> ofNullable(e.getName())
                            .map(v -> v + " (" + e.getId() + ")")
                            .orElse(e.getId()))
                .sorted()
                .collect(Collectors.toList());
    }

    @GetMapping
    public String get(Model model) {
        List<TransactionModel> transactions = transactionModelRepository.findAll();
        model.addAttribute("transactions", transactions);
        return "transactions/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(Model model,
                              @RequestParam(name = "portfolio", required = false) String portfolio,
                              @RequestParam(name = "transaction-id", required = false) String transactionId) {
        TransactionModel transaction;
        if (portfolio != null && transactionId != null) {
            transaction = transactionModelRepository.findById(portfolio, transactionId)
                    .orElseGet(TransactionModel::new);
        } else {
            transaction = new TransactionModel();
            transaction.setPortfolio(selectedPortfolio);
        }
        model.addAttribute("transaction", transaction);
        model.addAttribute("securities", securities);
        model.addAttribute("portfolios", portfolios);
        return "transactions/edit-form";
    }

    /**
     * Saves transaction to storage
     *
     * @param transaction transaction for save
     * @param model       model with result for exposing to view
     */
    @PostMapping
    public String postTransaction(@ModelAttribute @Valid TransactionModel transaction, Model model) {
        selectedPortfolio = transaction.getPortfolio();
        transactionModelRepository.saveAndFlush(transaction);
        model.addAttribute("transaction", transaction);
        return "transactions/view-single";
    }
}
