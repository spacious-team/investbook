package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.util.ArrayList;

public interface SecurityEventCashFlowRepository extends JpaRepository<SecurityEventCashFlowEntity, Integer> {

    @Query("SELECT cf FROM SecurityEventCashFlowEntity cf " +
            "WHERE cf.portfolio.portfolio = :portfolio " +
            "AND cf.security.isin = :isin " +
            "AND cf.cashFlowType.id = :#{#cashFlowType.ordinal()} " +
            "ORDER BY cf.timestamp")
    ArrayList<SecurityEventCashFlowEntity> findByPortfolioAndIsinAndCashFlowTypeOrderByTimestampAsc(
            @Param("portfolio") String portfolio,
            @Param("isin") String isin,
            @Param("cashFlowType") CashFlowType cashFlowType);

}