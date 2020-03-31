/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.PortfolioPropertyEntity;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.repository.PortfolioRepository;

@Component
@RequiredArgsConstructor
public class PortfolioPropertyConverter implements EntityConverter<PortfolioPropertyEntity, PortfolioProperty> {
    private final PortfolioRepository portfolioRepository;

    @Override
    public PortfolioPropertyEntity toEntity(PortfolioProperty property) {
        PortfolioEntity portfolioEntity = portfolioRepository.findById(property.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + property.getPortfolio()));

        PortfolioPropertyEntity entity = new PortfolioPropertyEntity();
        entity.setId(property.getId());
        entity.setPortfolio(portfolioEntity);
        entity.setTimestamp(property.getTimestamp());
        entity.setProperty(property.getProperty().name());
        entity.setValue(property.getValue());
        return  entity;
    }
}
