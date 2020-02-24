package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;
import ru.portfolio.portfolio.repository.PortfolioRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;

@Component
@RequiredArgsConstructor
public class SecurityEventCashFlowEntityConverter implements EntityConverter<SecurityEventCashFlowEntity, SecurityEventCashFlow> {
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final CashFlowTypeRepository cashFlowTypeRepository;

    @Override
    public SecurityEventCashFlowEntity toEntity(SecurityEventCashFlow eventCashFlow) {
        SecurityEntity securityEntity = null;
        if (eventCashFlow.getIsin() != null) {
            securityEntity = securityRepository.findByIsin(eventCashFlow.getIsin())
                    .orElseThrow(() -> new IllegalArgumentException("Ценная бумага с заданным ISIN не найдена: " + eventCashFlow.getIsin()));
        }
        PortfolioEntity portfolioEntity = portfolioRepository.findById(eventCashFlow.getPortfolio())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найден брокерский счет: " + eventCashFlow.getPortfolio()));
        CashFlowTypeEntity cashFlowTypeEntity = cashFlowTypeRepository.findById(eventCashFlow.getEventType().getType())
                .orElseThrow(() -> new IllegalArgumentException("В справочнике не найдено событие с типом: " + eventCashFlow.getEventType().getType()));

        SecurityEventCashFlowEntity entity = new SecurityEventCashFlowEntity();
        entity.setId(eventCashFlow.getId());
        entity.setPortfolio(portfolioEntity);
        entity.setTimestamp(eventCashFlow.getTimestamp());
        entity.setSecurity(securityEntity);
        entity.setCount(eventCashFlow.getCount());
        entity.setCashFlowType(cashFlowTypeEntity);
        entity.setValue(eventCashFlow.getValue());
        if(eventCashFlow.getCurrency() != null) entity.setCurrency(eventCashFlow.getCurrency());
        return entity;
    }
}
