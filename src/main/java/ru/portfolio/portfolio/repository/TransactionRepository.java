package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.TransactionEntity;

import java.util.ArrayList;
import java.util.Collection;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :portfolio " +
            "AND length(isin) = 12 " +
            "ORDER BY timestamp")
    Collection<String> findDistinctIsinByPortfolioOrderByTimestamp(@Param("portfolio") PortfolioEntity portfolio);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :portfolio " +
            "AND length(isin) <> 12 " +
            "ORDER BY timestamp")
    Collection<String> findDistinctDerivativeByPortfolioOrderByTimestamp(@Param("portfolio") PortfolioEntity portfolio);

    ArrayList<TransactionEntity> findBySecurityAndPortfolioOrderByTimestampAscIdAsc(SecurityEntity security, PortfolioEntity portfolio);
}
