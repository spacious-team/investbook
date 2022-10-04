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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.PortfolioPropertyModel;
import ru.investbook.web.forms.model.PortfolioPropertyTotalAssetsModel;
import ru.investbook.web.forms.model.filter.PortfolioPropertyFormFilterModel;
import ru.investbook.web.forms.service.PortfolioPropertyFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.Collection;
import java.util.function.Supplier;

@Controller
@RequestMapping("/portfolio-properties")
@RequiredArgsConstructor
public class PortfolioPropertyController {
    private final PortfolioPropertyFormsService portfolioPropertyFormsService;
    private final PortfolioRepository portfolioRepository;
    private volatile Collection<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
    }

    @GetMapping
    public String get(@ModelAttribute("filter") PortfolioPropertyFormFilterModel filter, Model model) {
        Page<PortfolioPropertyModel> page = portfolioPropertyFormsService.getPage(filter);
        model.addAttribute("page", new PageableWrapperModel<>(page));
        model.addAttribute("portfolios", portfolios);
        return "portfolio-properties/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") PortfolioPropertyFormFilterModel filter,
                         RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/portfolio-properties";
    }

    @GetMapping("/edit-form/total-assets")
    public String getTotalAssetsEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        PortfolioPropertyTotalAssetsModel property = (PortfolioPropertyTotalAssetsModel)
                getPortfolioProperty(id, PortfolioPropertyTotalAssetsModel::new);
        model.addAttribute("property", property);
        model.addAttribute("portfolios", portfolios);
        return "portfolio-properties/total-assets-edit-form";
    }

    private PortfolioPropertyModel getPortfolioProperty(Integer id,
                                                        Supplier<? extends PortfolioPropertyModel> newSupplier) {
        if (id != null) {
            return portfolioPropertyFormsService.getById(id)
                    .orElseGet(newSupplier);
        } else {
            PortfolioPropertyModel model = newSupplier.get();
            model.setPortfolio(selectedPortfolio);
            return model;
        }
    }

    @PostMapping("/total-assets")
    public String postCash(@Valid @ModelAttribute("property") PortfolioPropertyTotalAssetsModel property) {
        selectedPortfolio = property.getPortfolio();
        portfolioPropertyFormsService.save(property);
        return "portfolio-properties/total-assets-view-single";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model) {
        portfolioPropertyFormsService.delete(id);
        model.addAttribute("message", "Запись удалена");
        model.addAttribute("backLink", "/portfolio-properties");
        return "success";
    }
}
