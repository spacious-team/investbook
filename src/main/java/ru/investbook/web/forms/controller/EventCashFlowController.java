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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.EventCashFlowModel;
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;
import ru.investbook.web.forms.service.EventCashFlowFormsService;
import ru.investbook.web.forms.service.SecurityEventCashFlowFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.Collection;
import java.util.Optional;

@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventCashFlowController {
    private final EventCashFlowFormsService eventCashFlowFormsService;
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
        model.addAttribute("events", eventCashFlowFormsService.getAll());
        return "events/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("event", getEventCashFlow(id));
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("securities", securities);
        return "events/edit-form";
    }

    private EventCashFlowModel getEventCashFlow(Integer id) {
        if (id != null) {
            return eventCashFlowFormsService.getById(id)
                    .orElseGet(EventCashFlowModel::new);
        } else {
            EventCashFlowModel event = new EventCashFlowModel();
            event.setPortfolio(selectedPortfolio);
            return event;
        }
    }

    @PostMapping
    public String postEventCashFlow(@Valid EventCashFlowModel event, Model model) {
        selectedPortfolio = event.getPortfolio();
        if (event.getAttached() != null && event.isAttachedToSecurity()) {
            SecurityEventCashFlowModel attachedToSecurityModel = event.getAttached().toSecurityEventCashFlowModel();
            securityEventCashFlowFormsService.save(attachedToSecurityModel);
            Optional.ofNullable(event.getId()).ifPresent(eventCashFlowFormsService::delete);
            model.addAttribute("event", attachedToSecurityModel);
            return "security-events/view-single";
        }
        model.addAttribute("event", event);
        eventCashFlowFormsService.save(event);
        return "events/view-single";
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "id") Integer id, Model model) {
        eventCashFlowFormsService.delete(id);
        model.addAttribute("message", "Запись удалена");
        model.addAttribute("backLink", "/events");
        return "success";
    }
}
