package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class EventCashFlowEntityConverter implements EntityConverter<EventCashFlowEntity, EventCashFlow> {
    private final SecurityRepository securityRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public EventCashFlowEntity toEntity(EventCashFlow eventCashFlow) {
        SecurityEntity securityEntity = securityRepository.findByIsin(eventCashFlow.getIsin())
                .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ISIN не найдена: " + eventCashFlow.getIsin()));
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.findById(eventCashFlow.getEventType().getType())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найдено событие с типом: " + eventCashFlow.getEventType().getType()));

        EventCashFlowEntity entity = new EventCashFlowEntity();
        entity.setId(eventCashFlow.getId());
        entity.setTimestamp(eventCashFlow.getTimestamp());
        entity.setSecurity(securityEntity);
        entity.setCashFlowType(cashFlowTypeEntity);
        entity.setValue(eventCashFlow.getValue());
        if(eventCashFlow.getCurrency() != null) entity.setCurrency(eventCashFlow.getCurrency());
        return entity;
    }
}
