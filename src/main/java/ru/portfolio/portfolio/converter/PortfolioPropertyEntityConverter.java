package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.PortfolioPropertyEntity;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.repository.PortfolioRepository;

@Component
@RequiredArgsConstructor
public class PortfolioPropertyEntityConverter implements EntityConverter<PortfolioPropertyEntity, PortfolioProperty> {
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
