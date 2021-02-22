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
import ru.investbook.model.dto.SecurityQuoteModel;
import ru.investbook.model.repository.SecurityQuoteModelRepository;
import ru.investbook.repository.SecurityRepository;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import java.util.List;

@Controller
@RequestMapping("/security-quotes")
@RequiredArgsConstructor
public class SecurityQuoteController {
    private final SecurityQuoteModelRepository securityQuoteModelRepository;
    private final SecurityRepository securityRepository;
    private volatile List<String> securities;

    @PostConstruct
    public void start() {
        securities = ControllerHelper.getSecuritiesDescriptions(securityRepository);
    }

    @GetMapping
    public String get(Model model) {
        List<SecurityQuoteModel> quotes = securityQuoteModelRepository.findAll();
        model.addAttribute("quotes", quotes);
        return "security-quotes/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(Model model, @RequestParam(name = "id", required = false) Integer id) {
        SecurityQuoteModel quote;
        if (id != null) {
            quote = securityQuoteModelRepository.findById(id)
                    .orElseGet(SecurityQuoteModel::new);
        } else {
            quote = new SecurityQuoteModel();
        }
        model.addAttribute("quote", quote);
        model.addAttribute("securities", securities);
        return "security-quotes/edit-form";
    }

    @PostMapping
    public String postTransaction(@ModelAttribute @Valid SecurityQuoteModel quote, Model model) {
        securityQuoteModelRepository.saveAndFlush(quote);
        model.addAttribute("quote", quote);
        return "security-quotes/view-single";
    }
}
