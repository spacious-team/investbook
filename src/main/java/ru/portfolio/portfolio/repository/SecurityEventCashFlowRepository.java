package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;

import java.util.ArrayList;

public interface SecurityEventCashFlowRepository extends JpaRepository<SecurityEventCashFlowEntity, Integer> {

    ArrayList<SecurityEventCashFlowEntity> findByPortfolioPortfolioAndSecurityIsinAndCashFlowTypeIdOrderByTimestampAsc(
            String portfolio,
            String isin,
            int cashFlowType);

}