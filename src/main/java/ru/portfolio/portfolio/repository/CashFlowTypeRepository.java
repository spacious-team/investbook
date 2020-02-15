package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.dao.CashFlowTypeEntity;

import java.util.List;
import java.util.Optional;

public interface CashFlowTypeRepository extends JpaRepository<CashFlowTypeEntity, Integer> {
    @Override
    Optional<CashFlowTypeEntity> findById(Integer integer);

    @Override
    List<CashFlowTypeEntity> findAll();
}
