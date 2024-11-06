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

package ru.investbook.converter;

import org.spacious_team.broker.pojo.PortfolioCash;
import org.springframework.stereotype.Component;
import ru.investbook.entity.PortfolioCashEntity;

@Component
public class PortfolioCashConverter implements EntityConverter<PortfolioCashEntity, PortfolioCash>  {

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    @Override
    public PortfolioCashEntity toEntity(PortfolioCash cash) {
        PortfolioCashEntity entity = new PortfolioCashEntity();
        entity.setId(cash.getId());
        entity.setPortfolio(cash.getPortfolio());
        entity.setMarket(cash.getMarket());
        entity.setTimestamp(cash.getTimestamp());
        entity.setValue(cash.getValue());
        entity.setCurrency(cash.getCurrency());
        return entity;
    }

    @Override
    public PortfolioCash fromEntity(PortfolioCashEntity entity) {
        return PortfolioCash.builder()
                .id(entity.getId())
                .portfolio(entity.getPortfolio())
                .market(entity.getMarket())
                .timestamp(entity.getTimestamp())
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}
