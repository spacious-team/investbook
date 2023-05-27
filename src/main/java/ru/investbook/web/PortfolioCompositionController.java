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

package ru.investbook.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.service.AssetsAndCashService;
import ru.investbook.service.InvestmentProportionService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/charts")
@RequiredArgsConstructor
@Slf4j
public class PortfolioCompositionController {

    private final InvestmentProportionService investmentProportionService;
    private final AssetsAndCashService assetsAndCashService;

    @GetMapping("/sectors-pie-chart")
    public String getSectorsProportionPage(Model model, HttpServletRequest request) {
        try {
            Set<String> portfolios = assetsAndCashService.getActivePortfolios();
            Collection<Map<String, ?>> sectorsProportion = investmentProportionService.getSectorsProportion(portfolios)
                    .entrySet()
                    .stream()
                    .map(e -> Map.of("sector", e.getKey(), "investment", ((Number) e.getValue()).intValue()))
                    .collect(toList());
            int cash = assetsAndCashService.getTotalCashInRub(portfolios)
                    .map(Number::intValue)
                    .orElse(0);
            sectorsProportion.add(Map.of("sector", "Кеш", "investment", cash));
            model.addAttribute("sectorsProportion", sectorsProportion);
            model.addAttribute("request", request);
            return "charts/sectors-pie-chart";
        } catch (Exception e) {
            model.addAttribute("title", "Ошибка");
            model.addAttribute("message",
                    "При сборке круговой диаграммы секторального состава портфеля возникла ошибка: " + e.getMessage() +
                            ". Полное описание ошибки доступно в файле лога, обратитесь в техническую поддержку.");
            return "success";
        }
    }

    @GetMapping("/securities-pie-chart")
    public String getSecuritiesProportionPage(Model model, HttpServletRequest request) {
        try {
            Set<String> portfolios = assetsAndCashService.getActivePortfolios();
            Collection<Map<String, ?>> securitiesProportion = investmentProportionService.getSecuritiesProportion(portfolios)
                    .entrySet()
                    .stream()
                    .map(e -> Map.of("security", e.getKey(), "investment", ((Number) e.getValue()).intValue()))
                    .collect(toList());
            int cash = assetsAndCashService.getTotalCashInRub(portfolios)
                    .map(Number::intValue)
                    .orElse(0);
            securitiesProportion.add(Map.of("security", "Кеш", "investment", cash));
            model.addAttribute("securitiesProportion", securitiesProportion);
            model.addAttribute("request", request);
            return "charts/securities-pie-chart";
        } catch (Exception e) {
            model.addAttribute("title", "Ошибка");
            model.addAttribute("message",
                    "При сборке круговой диаграммы состава портфеля возникла ошибка: " + e.getMessage() +
                            ". Полное описание ошибки доступно в файле лога, обратитесь в техническую поддержку.");
            return "success";
        }
    }
}
