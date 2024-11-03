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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.ForeignExchangeRateRepository;
import ru.investbook.repository.TransactionRepository;
import ru.investbook.service.cbr.CbrForeignExchangeRateService;
import ru.investbook.web.forms.model.ForeignExchangeRateModel;
import ru.investbook.web.forms.model.PageableWrapperModel;
import ru.investbook.web.forms.model.filter.ForeignExchangeRateFormFilterModel;
import ru.investbook.web.forms.service.ForeignExchangeRateFormsService;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/foreign-exchange-rates")
@RequiredArgsConstructor
public class ForeignExchangeRateController {
    private final ForeignExchangeRateFormsService foreignExchangeRateFormsService;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final CbrForeignExchangeRateService cbrForeignExchangeRateService;
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public String get(@ModelAttribute("filter") ForeignExchangeRateFormFilterModel filter, Model model) {
        Page<ForeignExchangeRateModel> data = foreignExchangeRateFormsService.getPage(filter);
        model.addAttribute("page", new PageableWrapperModel<>(data));

        return "foreign-exchange-rates/table";
    }

    @PostMapping("/search")
    public String search(@ModelAttribute("filter") ForeignExchangeRateFormFilterModel filter,
                         RedirectAttributes attributes) {
        attributes.addFlashAttribute("filter", filter);
        return "redirect:/foreign-exchange-rates";
    }

    @GetMapping("/edit-form")
    public String getEditForm(@RequestParam(name = "date", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd")
                                      LocalDate date,
                              @RequestParam(name = "baseCurrency", required = false)
                                      String baseCurrency,
                              @RequestParam(name = "quoteCurrency", required = false)
                                      String quoteCurrency,
                              Model model) {
        model.addAttribute("rate", getForeignExchangeRate(date, baseCurrency, quoteCurrency));
        return "foreign-exchange-rates/edit-form";
    }

    private ForeignExchangeRateModel getForeignExchangeRate(LocalDate date, String baseCurrency, String quoteCurrency) {
        if (date != null && baseCurrency != null && quoteCurrency != null) {
            return foreignExchangeRateFormsService.getById(date, baseCurrency, quoteCurrency)
                    .orElseGet(ForeignExchangeRateModel::new);
        } else {
            return new ForeignExchangeRateModel();
        }
    }

    @PostMapping
    public String postForeignExchangeRate(@ModelAttribute("rate") @Valid ForeignExchangeRateModel rate) {
        foreignExchangeRateFormsService.save(rate);
        foreignExchangeRateService.invalidateCache();
        return "foreign-exchange-rates/view-single";
    }

    @GetMapping("update")
    public String updateForeignExchangeRate(Model model) {
        String message = updateForeignExchangeRateFromCbr();
        model.addAttribute("message", message);
        return "success";
    }

    public String updateForeignExchangeRateFromCbr() {
        cbrForeignExchangeRateService.updateFrom(getFirstTransactionDate());
        foreignExchangeRateService.invalidateCache();
        return "Официальные курсы обновлены по " + getLatestDateOfAllFxRateKnown() + " включительно";
    }

    private LocalDate getFirstTransactionDate() {
        return transactionRepository.findFirstByOrderByTimestampAsc()
                .map(e -> LocalDate.ofInstant(e.getTimestamp(), ZoneId.systemDefault()))
                .orElseGet(() -> LocalDate.of(2010, 1, 1));
    }

    private LocalDate getLatestDateOfAllFxRateKnown() {
        return foreignExchangeRateRepository.findByMaxPkDateGroupByPkCurrencyPair()
                .stream()
                .min(LocalDate::compareTo)
                .orElseGet(() -> LocalDate.of(2010, 1, 1));
    }

    @GetMapping("/delete")
    public String delete(@RequestParam(name = "date")
                              @DateTimeFormat(pattern = "yyyy-MM-dd")
                                      LocalDate date,
                              @RequestParam(name = "baseCurrency")
                                      String baseCurrency,
                              @RequestParam(name = "quoteCurrency")
                                      String quoteCurrency,
                              Model model) {
        foreignExchangeRateFormsService.delete(date, baseCurrency, quoteCurrency);
        foreignExchangeRateService.invalidateCache();
        model.addAttribute("message", "Обменный курс удален");
        model.addAttribute("backLink", "/foreign-exchange-rates");
        return "success";
    }
}
