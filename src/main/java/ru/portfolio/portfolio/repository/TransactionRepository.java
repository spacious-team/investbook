package ru.portfolio.portfolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.Portfolio;

import java.util.ArrayList;
import java.util.Collection;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.portfolio} " +
            "AND length(isin) = 12 " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.portfolio} " +
            "AND length(isin) <> 12 " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctDerivativeByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    ArrayList<TransactionEntity> findBySecurityIsinAndPortfolioPortfolioOrderByTimestampAscIdAsc(String isin,
                                                                                                 String portfolio);
}
