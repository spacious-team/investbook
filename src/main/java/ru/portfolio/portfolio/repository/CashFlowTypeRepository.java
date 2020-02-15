package ru.portfolio.portfolio.repository;

import org.springframework.data.repository.CrudRepository;
import ru.portfolio.portfolio.dao.CashFlowTypeEntity;

import java.util.Optional;

public interface CashFlowTypeRepository extends CrudRepository<CashFlowTypeEntity, Integer> {
    @Override
    Optional<CashFlowTypeEntity> findById(Integer integer);

    @Override
    Iterable<CashFlowTypeEntity> findAll();
}
