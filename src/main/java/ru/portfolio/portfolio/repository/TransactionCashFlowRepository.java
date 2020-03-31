package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntityPK;

import java.util.List;
import java.util.Optional;

public interface TransactionCashFlowRepository extends JpaRepository<TransactionCashFlowEntity, TransactionCashFlowEntityPK> {

    List<TransactionCashFlowEntity> findByTransactionId(long transactionId);

    Optional<TransactionCashFlowEntity> findByTransactionCashFlowIdTransactionIdAndCashFlowTypeId(long transactionId,
                                                                                                  int cashFlowType);
}
