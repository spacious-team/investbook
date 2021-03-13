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
import ru.investbook.model.dto.PortfolioPropertyModel;
import ru.investbook.model.repository.PortfolioPropertyModelRepository;
import ru.investbook.repository.PortfolioRepository;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/portfolio-properties")
@RequiredArgsConstructor
public class PortfolioPropertyController {
    private final PortfolioPropertyModelRepository portfolioPropertyModelRepository;
    private final PortfolioRepository portfolioRepository;
    private volatile List<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
    }

    @GetMapping
    public String get(Model model) {
        model.addAttribute("properties", portfolioPropertyModelRepository.findAll());
        return "portfolio-properties/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("property", getPortfolioProperty(id));
        model.addAttribute("portfolios", portfolios);
        return "portfolio-properties/edit-form";
    }

    private PortfolioPropertyModel getPortfolioProperty(Integer id) {
        if (id != null) {
            return portfolioPropertyModelRepository.findById(id)
                    .orElseGet(PortfolioPropertyModel::new);
        } else {
            PortfolioPropertyModel property = new PortfolioPropertyModel();
            property.setPortfolio(selectedPortfolio);
            return property;
        }
    }

    @PostMapping
    public String postTransaction(@Valid @ModelAttribute("property") PortfolioPropertyModel property) {
        selectedPortfolio = property.getPortfolio();
        portfolioPropertyModelRepository.saveAndFlush(property);
        return "portfolio-properties/view-single";
    }
}
