package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;

import java.util.ArrayList;

public interface EventCashFlowRepository extends JpaRepository<EventCashFlowEntity, Integer> {

    ArrayList<EventCashFlowEntity> findByPortfolioIdAndCashFlowTypeIdOrderByTimestamp(String portfolio,
                                                                                      int cashFlowType);
}
