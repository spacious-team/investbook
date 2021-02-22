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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.model.dto.ForeignExchangeRateModel;
import ru.investbook.model.repository.ForeignExchangeRateModelRepository;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/foreign-exchange-rates")
@RequiredArgsConstructor
public class ForeignExchangeRateController {
    private final ForeignExchangeRateModelRepository foreignExchangeRateModelRepository;

    @GetMapping
    public String get(Model model) {
        List<ForeignExchangeRateModel> rates = foreignExchangeRateModelRepository.findAll();
        model.addAttribute("rates", rates);
        return "foreign-exchange-rates/table";
    }

    @GetMapping("/edit-form")
    public String getEditForm(Model model,
                              @RequestParam(name = "date", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd")
                                      LocalDate date,
                              @RequestParam(name = "baseCurrency", required = false)
                                      String baseCurrency,
                              @RequestParam(name = "quoteCurrency", required = false)
                                      String quoteCurrency) {
        ForeignExchangeRateModel rate;
        if (date != null && baseCurrency != null && quoteCurrency != null) {
            rate = foreignExchangeRateModelRepository.findById(date, baseCurrency, quoteCurrency)
                    .orElseGet(ForeignExchangeRateModel::new);
        } else {
            rate = new ForeignExchangeRateModel();
        }
        model.addAttribute("rate", rate);
        return "foreign-exchange-rates/edit-form";
    }

    @PostMapping
    public String postTransaction(@ModelAttribute @Valid ForeignExchangeRateModel rate, Model model) {
        foreignExchangeRateModelRepository.saveAndFlush(rate);
        model.addAttribute("rate", rate);
        return "foreign-exchange-rates/view-single";
    }
}
