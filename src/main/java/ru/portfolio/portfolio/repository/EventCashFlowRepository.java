package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;
import ru.portfolio.portfolio.entity.PortfolioEntity;

import java.util.ArrayList;

public interface EventCashFlowRepository extends JpaRepository<EventCashFlowEntity, Integer> {

    ArrayList<EventCashFlowEntity> findByPortfolioAndCashFlowTypeOrderByTimestamp(PortfolioEntity portfolioEntity,
                                                                                  CashFlowTypeEntity type);
}
