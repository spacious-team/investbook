package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.util.ArrayList;
import java.util.List;

public interface SecurityEventCashFlowRepository extends JpaRepository<SecurityEventCashFlowEntity, Integer> {

    @Query("SELECT cf FROM SecurityEventCashFlowEntity cf WHERE cf.security.isin = :isin AND cf.cashFlowType.id = :#{#cashFlowType.ordinal()}")
    List<SecurityEventCashFlowEntity> findByIsinAndCashFlowType(@Param("isin") String isin, @Param("cashFlowType") CashFlowType cashFlowType);

    @Query("SELECT cf FROM SecurityEventCashFlowEntity cf WHERE cf.security.isin = :isin AND cf.cashFlowType.id = :#{#cashFlowType.ordinal()} ORDER BY cf.timestamp")
    ArrayList<SecurityEventCashFlowEntity> findByIsinAndCashFlowTypeOOrderByTimestampAsc(@Param("isin") String isin,
                                                                                         @Param("cashFlowType") CashFlowType cashFlowType);

}
