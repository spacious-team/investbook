package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
}
