package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;

import java.util.Optional;

public interface CashFlowTypeRepository extends JpaRepository<CashFlowTypeEntity, Integer> {
    @Override
    Optional<CashFlowTypeEntity> findById(Integer integer);
}
