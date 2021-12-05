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

import ru.investbook.entity.PortfolioEntity;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.web.forms.model.SecurityType;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.investbook.web.forms.model.SecurityHelper.getSecurityDescription;

public class ControllerHelper {

    public static Set<String> getPortfolios(PortfolioRepository portfolioRepository) {
        return portfolioRepository.findAll()
                .stream()
                .map(PortfolioEntity::getId)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<String> getActivePortfolios(PortfolioRepository portfolioRepository) {
        return portfolioRepository.findByEnabledIsTrue()
                .stream()
                .map(PortfolioEntity::getId)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static Set<String> getInactivePortfolios(PortfolioRepository portfolioRepository) {
        return portfolioRepository.findByEnabledIsFalse()
                .stream()
                .map(PortfolioEntity::getId)
                .collect(Collectors.toSet());
    }

    public static Set<String> getSecuritiesDescriptions(SecurityRepository securityRepository) {
        return securityRepository.findAll()
                .stream()
                .map(e -> getSecurityDescription(
                        e.getId(),
                        e.getName(),
                        SecurityType.valueOf(e.getType())))
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
