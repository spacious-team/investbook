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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.entity.StockMarketIndexEntity;
import ru.investbook.repository.StockMarketIndexRepository;
import ru.investbook.service.Sp500Service;

@Controller
@RequestMapping("/sp500")
@RequiredArgsConstructor
public class Sp500Controller {

    private final Sp500Service sp500Service;
    private final StockMarketIndexRepository stockMarketIndexRepository;

    @GetMapping("update")
    public String updateSp500(Model model) {
        String message = updateSp500Index();
        model.addAttribute("title", "S&P 500");
        model.addAttribute("message", message);
        return "success";
    }

    public String updateSp500Index() {
        sp500Service.update();
        return stockMarketIndexRepository.findFirstBySp500NotNullOrderByDateDesc()
                .map(StockMarketIndexEntity::getDate)
                .map(date -> "Индекс обновлен обновлен по " + date + " включительно")
                .orElse("Запрос выполнен, но сервер https://www.spglobal.com/ не вернул данные");
    }
}
