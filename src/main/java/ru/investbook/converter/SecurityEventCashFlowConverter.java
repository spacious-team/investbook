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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.springframework.stereotype.Component;
import ru.investbook.entity.CashFlowTypeEntity;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.CashFlowTypeRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class SecurityEventCashFlowConverter implements EntityConverter<SecurityEventCashFlowEntity, SecurityEventCashFlow> {
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public SecurityEventCashFlowEntity toEntity(SecurityEventCashFlow eventCashFlow) {
        SecurityEntity securityEntity = securityRepository.findById(eventCashFlow.getSecurity())
                .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ID не найдена: " + eventCashFlow.getSecurity()));
        PortfolioEntity portfolioEntity = portfolioRepository.findById(eventCashFlow.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + eventCashFlow.getPortfolio()));
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.findById(eventCashFlow.getEventType().getId())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найдено событие с типом: " + eventCashFlow.getEventType().getId()));

        SecurityEventCashFlowEntity entity = new SecurityEventCashFlowEntity();
        entity.setId(eventCashFlow.getId());
        entity.setPortfolio(portfolioEntity);
        entity.setTimestamp(eventCashFlow.getTimestamp());
        entity.setSecurity(securityEntity);
        entity.setCount(eventCashFlow.getCount());
        entity.setCashFlowType(cashFlowTypeEntity);
        entity.setValue(eventCashFlow.getValue());
        if (eventCashFlow.getCurrency() != null) entity.setCurrency(eventCashFlow.getCurrency());
        return entity;
    }

    @Override
    public SecurityEventCashFlow fromEntity(SecurityEventCashFlowEntity entity) {
        return SecurityEventCashFlow.builder()
                .id(entity.getId())
                .portfolio(entity.getPortfolio().getId())
                .timestamp(entity.getTimestamp())
                .eventType(CashFlowType.valueOf(entity.getCashFlowType().getId()))
                .security(entity.getSecurity().getId())
                .count(entity.getCount())
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .build();
    }
}
