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
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;
import ru.investbook.web.forms.service.SecurityEventCashFlowFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.Collection;

@Controller
@RequestMapping("/security-events")
@RequiredArgsConstructor
public class SecurityEventCashFlowController {
    private final SecurityEventCashFlowFormsService securityEventCashFlowFormsService;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private volatile Collection<String> securities;
    private volatile Collection<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(Model model) {
        model.addAttribute("events", securityEventCashFlowFormsService.getAll());
        return "security-events/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("event", getSecurityEventCashFlow(id));
        model.addAttribute("securities", securities);
        model.addAttribute("portfolios", portfolios);
        return "security-events/edit-form";
    }

    private SecurityEventCashFlowModel getSecurityEventCashFlow(Integer id) {
        if (id != null) {
            return securityEventCashFlowFormsService.getById(id)
                    .orElseGet(SecurityEventCashFlowModel::new);
        } else {
            SecurityEventCashFlowModel event = new SecurityEventCashFlowModel();
            event.setPortfolio(selectedPortfolio);
            return event;
        }
    }

    @PostMapping
    public String postTransaction(@Valid @ModelAttribute("event") SecurityEventCashFlowModel event) {
        selectedPortfolio = event.getPortfolio();
        securityEventCashFlowFormsService.save(event);
        return "security-events/view-single";
    }
}
