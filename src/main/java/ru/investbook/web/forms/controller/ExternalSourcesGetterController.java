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
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/external-sources")
@Slf4j
@RequiredArgsConstructor
public class ExternalSourcesGetterController {
    private static final String REDIRECT_URL = "update-fx-quote-sectors";
    private final ForeignExchangeRateController foreignExchangeRateController;
    private final SecurityQuoteController securityQuoteController;
    private final Sp500Controller sp500Controller;
    private final SecurityDescriptionController securityDescriptionController;


    @GetMapping("/get")
    public String updateSp500AndCallRedirect(Model model) {
        model.addAttribute("successURL", "/external-sources/" + REDIRECT_URL);
        return sp500Controller.updateSp500(model);
    }

    @GetMapping(REDIRECT_URL)
    public String updateFxAndQuoteAndSectors(@RequestParam(name = "message", required = false) @Nullable String sp500Message,
                                             Model model) throws ExecutionException, InterruptedException {
        Collection<String> messages = new ArrayList<>();
        Optional.ofNullable(sp500Message)
                .ifPresent(messages::add);
        messages.add(foreignExchangeRateController.updateForeignExchangeRateFromCbr());
        messages.add(securityQuoteController.updateQuoteFromMoexIssApi());
        messages.add(securityDescriptionController.updateSectorsFromSmartLab(false));
        model.addAttribute("message", String.join(".<br>", messages));
        model.addAttribute("backLink", "/forms.html");
        return "success";
    }
}
