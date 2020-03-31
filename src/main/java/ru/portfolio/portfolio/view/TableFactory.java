package ru.portfolio.portfolio.view;

import ru.portfolio.portfolio.pojo.Portfolio;

public interface TableFactory {
    Table create(Portfolio portfolio);
}
