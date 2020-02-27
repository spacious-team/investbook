package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;

import java.util.Collection;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction WHERE portfolio = :portfolio ORDER BY TIMESTAMP;")
    Collection<String> findDistinctIsinByPortfolio(@Param("portfolio") PortfolioEntity portfolio);
}
