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
import ru.investbook.model.dto.EventCashFlowModel;
import ru.investbook.model.repository.EventCashFlowModelRepository;
import ru.investbook.repository.PortfolioRepository;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventCashFlowController {
    private final EventCashFlowModelRepository eventCashFlowModelRepository;
    private final PortfolioRepository portfolioRepository;
    private volatile List<String> portfolios;
    private volatile String selectedPortfolio;

    @PostConstruct
    public void start() {
        portfolios = ControllerHelper.getPortfolios(portfolioRepository);
    }

    @GetMapping
    public String get(Model model) {
        model.addAttribute("events", eventCashFlowModelRepository.findAll());
        return "events/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "id", required = false) Integer id, Model model) {
        model.addAttribute("event", getEventCashFlow(id));
        model.addAttribute("portfolios", portfolios);
        return "events/edit-form";
    }

    private EventCashFlowModel getEventCashFlow(Integer id) {
        if (id != null) {
            return eventCashFlowModelRepository.findById(id)
                    .orElseGet(EventCashFlowModel::new);
        } else {
            EventCashFlowModel event = new EventCashFlowModel();
            event.setPortfolio(selectedPortfolio);
            return event;
        }
    }

    @PostMapping
    public String postTransaction(@Valid @ModelAttribute("event") EventCashFlowModel event) {
        selectedPortfolio = event.getPortfolio();
        eventCashFlowModelRepository.saveAndFlush(event);
        return "events/view-single";
    }
}
