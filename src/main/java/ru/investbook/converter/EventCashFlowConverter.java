/*
 * InvestBook
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

package ru.investbook.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.entity.CashFlowTypeEntity;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.EventCashFlow;
import ru.investbook.repository.CashFlowTypeRepository;
import ru.investbook.repository.PortfolioRepository;

@Component
@RequiredArgsConstructor
public class EventCashFlowConverter implements EntityConverter<EventCashFlowEntity, EventCashFlow> {
    private final PortfolioRepository portfolioRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public EventCashFlowEntity toEntity(EventCashFlow eventCashFlow) {
        PortfolioEntity portfolioEntity = portfolioRepository.findById(eventCashFlow.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + eventCashFlow.getPortfolio()));
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.findById(eventCashFlow.getEventType().getId())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найдено событие с типом: " + eventCashFlow.getEventType().getId()));

        EventCashFlowEntity entity = new EventCashFlowEntity();
        entity.setId(eventCashFlow.getId());
        entity.setPortfolio(portfolioEntity);
        entity.setTimestamp(eventCashFlow.getTimestamp());
        entity.setCashFlowType(cashFlowTypeEntity);
        entity.setValue(eventCashFlow.getValue());
        if(eventCashFlow.getCurrency() != null) entity.setCurrency(eventCashFlow.getCurrency());
        if (eventCashFlow.getDescription() != null &&! eventCashFlow.getDescription().isEmpty()) {
            entity.setDescription(eventCashFlow.getDescription());
        }
        return entity;
    }

    @Override
    public EventCashFlow fromEntity(EventCashFlowEntity entity) {
        return EventCashFlow.builder()
                .id(entity.getId())
                .portfolio(entity.getPortfolio().getId())
                .timestamp(entity.getTimestamp())
                .eventType(CashFlowType.valueOf(entity.getCashFlowType().getId()))
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .build();
    }
}
