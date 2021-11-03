/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.forms.model.ArchivedPortfolioModel;

import javax.validation.Valid;
import java.util.Set;

import static ru.investbook.web.ControllerHelper.getInactivePortfolios;
import static ru.investbook.web.ControllerHelper.getPortfolios;

@Controller
@RequestMapping("/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioRepository portfolioRepository;

    @GetMapping("/archive")
    public String get(Model model, @ModelAttribute("archive") ArchivedPortfolioModel archive) {
        Set<String> allPortfolios = getPortfolios(portfolioRepository);
        model.addAttribute("allPortfolios", allPortfolios);
        archive.setPortfolios(getInactivePortfolios(portfolioRepository));
        return "portfolios/archive";
    }

    @PostMapping("/archive")
    public String postEventCashFlow(@Valid @ModelAttribute("archive") ArchivedPortfolioModel archive) {
        getPortfolios(portfolioRepository)
                .forEach(portfolio -> {
                    var isEnabled = !archive.getPortfolios().contains(portfolio);
                    portfolioRepository.setEnabledForPortfolio(portfolio, isEnabled);
                });
        return "success";
    }
}
