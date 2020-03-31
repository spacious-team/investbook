package ru.portfolio.portfolio.converter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.pojo.Portfolio;

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
