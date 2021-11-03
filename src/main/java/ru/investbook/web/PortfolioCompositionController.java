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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.report.ViewFilter;
import ru.investbook.service.AssetsAndCashService;
import ru.investbook.service.InvestmentProportionService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/portfolio-composition")
@RequiredArgsConstructor
@Slf4j
public class PortfolioCompositionController {

    private final InvestmentProportionService investmentProportionService;
    private final AssetsAndCashService assetsAndCashService;

    @GetMapping
    public String getPage(Model model) {
        try {
            Set<String> portfolios = assetsAndCashService.getActivePortfolios();
            ViewFilter currentState = ViewFilter.builder()
                    .portfolios(portfolios)
                    .build();
            Collection<Map<String, ?>> investmentProportion = investmentProportionService.getSectorProportions(currentState)
                    .entrySet()
                    .stream()
                    .map(e -> Map.of("sector", e.getKey(), "investment", ((Number) e.getValue()).intValue()))
                    .collect(toList());
            int cash = assetsAndCashService.getTotalCash(portfolios)
                    .map(Number::intValue)
                    .orElse(0);
            investmentProportion.add(Map.of("sector", "Кеш", "investment", cash));
            model.addAttribute("investmentProportion", investmentProportion);
            return "portfolio-composition";
        } catch (Exception e) {
            model.addAttribute("title", "Ошибка");
            model.addAttribute("message",
                    "При сборке круговой диаграммы состава портфеля возникла ошибка: " + e.getMessage() +
                            ". Полное описание ошибки доступно в файле лога, обратитесь в техническую поддержку.");
            return "success";
        }
    }
}
