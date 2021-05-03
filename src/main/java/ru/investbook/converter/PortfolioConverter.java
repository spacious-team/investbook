/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.converter;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.stereotype.Component;
import ru.investbook.entity.PortfolioEntity;

@Component
@RequiredArgsConstructor
public class PortfolioConverter implements EntityConverter<PortfolioEntity, Portfolio> {
    @Override
    public PortfolioEntity toEntity(Portfolio pojo) {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(pojo.getId());
        return entity;
    }

    @Override
    public Portfolio fromEntity(PortfolioEntity entity) {
        return Portfolio.builder()
                .id(entity.getId())
                .build();
    }
}
