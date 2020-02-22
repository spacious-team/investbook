package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;

public interface EventCashFlowRepository extends JpaRepository<EventCashFlowEntity, Integer> {
}
