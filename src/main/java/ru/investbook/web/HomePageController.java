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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class HomePageController {

    private final BuildProperties buildProperties;
    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final ForeignExchangeRateService foreignExchangeRateService;

    @GetMapping
    public String index(Model model) {
        List<String> portfolios = getPortfolios();
        model.addAttribute("transactionsCount", transactionRepository.count());
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("assets", getAssets(portfolios));
        model.addAttribute("cashBalance", getTotalCash(portfolios));
        model.addAttribute("buildProperties", buildProperties);
        return "index";
    }

    @GetMapping("shutdown")
    @ResponseBody
    public String shutdown() {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> System.exit(0), 3, TimeUnit.SECONDS);
        return "Приложение остановлено";
    }

    private List<String> getPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toList());
    }

    private BigDecimal getAssets(Collection<String> portfolios) {
        return portfolios.stream()
                .map(portfolio -> portfolioPropertyRepository.findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(
                        portfolio, PortfolioPropertyType.TOTAL_ASSETS_RUB.name())) // TODO sum with TOTAL_ASSETS_USD
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(HomePageController::parseTotalAssetsIfCan)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    private static BigDecimal parseTotalAssetsIfCan(PortfolioPropertyEntity entity) {
        try {
            return BigDecimal.valueOf(
                    Double.parseDouble(
                            entity.getValue()));
        } catch (Exception e) {
            log.error("Значение должно содержать число, сохранено {}", entity);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getTotalCash(Collection<String> portfolios) {
        return portfolios.stream()
                .map(portfolio -> portfolioPropertyRepository.findFirstByPortfolioIdAndPropertyOrderByTimestampDesc(
                        portfolio, PortfolioPropertyType.CASH.name()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(HomePageController::groupByCurrency)
                .map(this::convertToRubAndSum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    /**
     * Returns summed cash values
     */
    // currency -> value
    private static Map<String, BigDecimal> groupByCurrency(PortfolioPropertyEntity e) {
        try {
            return PortfolioCash.deserialize(e.getValue())
                    .stream()
                    .collect(groupingBy(PortfolioCash::getCurrency,
                            reducing(BigDecimal.ZERO, PortfolioCash::getValue, BigDecimal::add)));
        } catch (Exception ex) {
            log.warn("Ошибка при десериализации свойства: {}", e.getValue(), ex);
            return Collections.emptyMap();
        }
    }

    private BigDecimal convertToRubAndSum(Map<String, BigDecimal> values) {
        return values.entrySet()
               .stream()
               .map(e -> foreignExchangeRateService.convertValueToCurrency(e.getValue(), e.getKey(), RUB))
               .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
