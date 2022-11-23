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
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.ControllerHelper;
import ru.investbook.web.forms.model.EventCashFlowModel;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.filter.EventCashFlowFormFilterModel;
import ru.investbook.web.forms.service.EventCashFlowFormsService;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.Collection;

@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventCashFlowController {
    private final EventCashFlowFormsService eventCashFlowFormsService;
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
    public String get(@ModelAttribute("filter") EventCashFlowFormFilterModel filter, Model model) {
        Page<EventCashFlowModel> data = eventCashFlowFormsService.getPage(filter);
        portfolios = ControllerHelper.getPortfolios(portfolioRepository); // update portfolios for filter
        model.addAttribute("page", new PageableWrapperModel<>(data));
        model.addAttribute("portfolios", portfolios);

        return "events/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") EventCashFlowFormFilterModel filter, RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/events";
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
    public String postEventCashFlow(@Valid @ModelAttribute("event") EventCashFlowModel event) {
        selectedPortfolio = event.getPortfolio();
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
