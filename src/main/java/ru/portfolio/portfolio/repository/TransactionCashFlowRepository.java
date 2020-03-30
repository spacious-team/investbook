package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntityPK;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.util.List;
import java.util.Optional;

public interface TransactionCashFlowRepository extends JpaRepository<TransactionCashFlowEntity, TransactionCashFlowEntityPK> {

    List<TransactionCashFlowEntity> findByTransactionId(long transactionId);

    @Query("SELECT cf FROM TransactionCashFlowEntity cf WHERE cf.transactionCashFlowId.transactionId = :transactionId AND cf.cashFlowType.id = :#{#cashFlowType.ordinal()}")
    Optional<TransactionCashFlowEntity> findByTransactionIdAndCashFlowType(@Param("transactionId") long transactionId, @Param("cashFlowType") CashFlowType cashFlowType);
}
