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
import org.springframework.web.bind.annotation.RequestParam;
import ru.investbook.report.ViewFilter;
import ru.investbook.service.InvestmentProportionService;
import ru.investbook.service.SecuritySectorService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/portfolio-composition")
@RequiredArgsConstructor
@Slf4j
public class PortfolioCompositionController {

    private final InvestmentProportionService investmentProportionService;
    private final SecuritySectorService securitySectorService;
    private final HomePageController homePageController;
    private final ViewFilter currentState = ViewFilter.builder().build();

    @GetMapping
    public String getPage(@RequestParam(name = "upload-sectors", required = false, defaultValue = "false")
                                  boolean uploadSectors,
                          Model model) {
        try {
            if (uploadSectors) {
                securitySectorService.uploadAndUpdateSecuritySectors();
            }
            Collection<Map<String, ?>> investmentProportion = investmentProportionService.getSectorProportions(currentState)
                    .entrySet()
                    .stream()
                    .map(e -> Map.of("sector", e.getKey(), "investment", ((Number) e.getValue()).intValue()))
                    .collect(toList());
            List<String> portfolios = homePageController.getPortfolios();
            investmentProportion.add(
                    Map.of("sector", "Кеш",
                            "investment", homePageController.getTotalCash(portfolios).intValue()));
            model.addAttribute("investmentProportion", investmentProportion);
            return "portfolio-composition";
        } catch (Exception e) {
            model.addAttribute("title", "Ошибка");
            model.addAttribute("message",
                    "При сборке круговой диаграмы состава портфеля возникла ошибка: " + e.getMessage() +
                            ". Полное описание ошибки доступно в файле лога, обратитесь в техническую поддержку.");
            return "success";
        }
    }
}
