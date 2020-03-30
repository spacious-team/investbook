package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;
import ru.portfolio.portfolio.repository.PortfolioRepository;

@Component
@RequiredArgsConstructor
public class EventCashFlowEntityConverter implements EntityConverter<EventCashFlowEntity, EventCashFlow> {
    private final PortfolioRepository portfolioRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public EventCashFlowEntity toEntity(EventCashFlow eventCashFlow) {
        PortfolioEntity portfolioEntity = portfolioRepository.findById(eventCashFlow.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + eventCashFlow.getPortfolio()));
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.findById(eventCashFlow.getEventType().getType())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найдено событие с типом: " + eventCashFlow.getEventType().getType()));

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
                .portfolio(entity.getPortfolio().getPortfolio())
                .timestamp(entity.getTimestamp())
                .eventType(CashFlowType.valueOf(entity.getCashFlowType().getId()))
                .value(entity.getValue())
                .currency(entity.getCurrency())
                .description(entity.getDescription())
                .build();
    }
}
