package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;

public interface CashFlowTypeRepository extends JpaRepository<CashFlowTypeEntity, Integer> {
}
