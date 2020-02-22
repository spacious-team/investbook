package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntityPK;

import java.util.List;

public interface TransactionCashFlowRepository extends JpaRepository<TransactionCashFlowEntity, TransactionCashFlowEntityPK> {
    List<TransactionCashFlowEntity> findByTransactionId(int transactionId);
}
