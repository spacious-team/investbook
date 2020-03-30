package ru.portfolio.portfolio.view;

import ru.portfolio.portfolio.entity.PortfolioEntity;

public interface ProfitTableFactory {
    ProfitTable create(PortfolioEntity portfolio);
}
