package ru.portfolio.portfolio.view;

import ru.portfolio.portfolio.entity.PortfolioEntity;

public interface TableFactory {
    Table create(PortfolioEntity portfolio);
}
