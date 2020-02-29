package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;

import java.util.Collection;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction WHERE portfolio = :portfolio ORDER BY timestamp")
    Collection<String> findDistinctIsinByPortfolioOrderByTimestamp(@Param("portfolio") PortfolioEntity portfolio);

    Collection<TransactionEntity> findBySecurityAndPortfolioOrderByTimestampAsc(SecurityEntity security, PortfolioEntity portfolio);
}
