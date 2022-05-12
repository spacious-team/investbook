/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.repository;

import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.TransactionEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {

    Optional<TransactionEntity> findFirstByOrderByTimestampAsc();

    Optional<TransactionEntity> findFirstByOrderByTimestampDesc();

    Optional<TransactionEntity> findFirstBySecurityIdOrderByTimestampDesc(Integer securityId);

    List<TransactionEntity> findByPortfolio(String portfolio);

    List<TransactionEntity> findByTradeId(String tradeId);

    Optional<TransactionEntity> findByPortfolioAndTradeId(String portfolio, String tradeId);

    List<TransactionEntity> findByPortfolioInOrderByPortfolioAscTimestampDescSecurityIdAsc(
            Collection<String> portfolios);

    /**
     * Returns stock market share, bonds ISINs and assets
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT t1.security FROM transaction as t1
            JOIN transaction_cash_flow as t2
                ON t1.id = t2.transaction_id
            JOIN security as s
                ON t1.security = s.id
            WHERE s.type IN (0, 1, 2, 5)
                AND t2.type = 1
                AND t1.portfolio IN (:portfolios)
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctSecurityByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns stock market share, bonds ISINs and assets
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT t1.security FROM transaction as t1
            JOIN transaction_cash_flow as t2
                ON t1.id = t2.transaction_id
            JOIN security as s
                ON t1.security = s.id
            WHERE s.type IN (0, 1, 2, 5)
                AND t2.type = 1
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctSecurityByCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 3
                AND t.portfolio IN (:portfolios)
                AND t.timestamp between :from AND :to
            ORDER BY t.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctDerivativeByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 3
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 4
                AND portfolio IN (:portfolios)
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 4
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByTimestampBetweenOrderByTimestampDesc(
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * @param currencyPair in USDRUB format
     * @return all contracts like USDRUB_TOD, USDRUB_TOM and others
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 4
                AND portfolio IN (:portfolios)
                AND s.ticker LIKE CONCAT(:currencyPair, '\\_%')
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    List<Integer> findDistinctFxContractByPortfolioInAndCurrencyPairAndTimestampBetween(
            @Param("portfolios") Collection<String> portfolios,
            @Param("currencyPair") String currencyPair,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * @param currencyPair in USDRUB format
     * @return all contracts like USDRUB_TOD, USDRUB_TOM and others
     */
    @Query(nativeQuery = true, value = """
            SELECT distinct t.security FROM transaction as t
            JOIN security as s
                ON t.security = s.id
            WHERE s.type = 4
                AND s.ticker LIKE CONCAT(:currencyPair, '\\_%')
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    List<Integer> findDistinctFxContractByCurrencyPairAndTimestampBetween(
            @Param("currencyPair") String currencyPair,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT t1.security FROM transaction as t1
            JOIN transaction_cash_flow as t2
                ON t1.id = t2.transaction_id
            JOIN security as s
                ON t1.security = s.id
            WHERE s.type = 4
                AND t2.type = 1
                AND t1.portfolio IN (:portfolios)
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = """
            SELECT DISTINCT t1.security FROM transaction as t1
            JOIN transaction_cash_flow as t2
                ON t1.id = t2.transaction_id
            JOIN security as s
                ON t1.security = s.id
            WHERE s.type = 4
                AND t2.type = 1
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    List<TransactionEntity> findBySecurityIdAndPortfolioInAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
            Integer securityId,
            Collection<String> portfolio,
            Instant fromDate,
            Instant toDate);

    List<TransactionEntity> findBySecurityIdAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
            Integer securityId,
            Instant fromDate,
            Instant toDate);

    /**
     * Return first security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndPortfolioAndTimestampBetweenOrderByTimestampAsc(
            Integer securityId,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndPortfolioAndTimestampBetweenOrderByTimestampDesc(
            Integer securityId,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return security total buy count
     */
    @Query(nativeQuery = true, value = """
            SELECT sum(count) FROM transaction
            WHERE portfolio = :#{#portfolio.id}
                AND security = :#{#security.id}
                AND timestamp between :from AND :to
                AND count > 0
            """)
    Long findBySecurityIdAndPortfolioAndTimestampBetweenBuyCount(
            @Param("security") Security security,
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Return security total cell count
     */
    @Query(nativeQuery = true, value = """
            SELECT abs(sum(count)) FROM transaction
            WHERE portfolio = :#{#portfolio.id}
                AND security = :#{#security.id}
                AND timestamp between :from AND :to
                AND count < 0
            """)
    Long findBySecurityIdAndPortfolioAndTimestampBetweenCellCount(
            @Param("security") Security security,
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    @Query("""
            SELECT t FROM TransactionEntity t
            LEFT OUTER JOIN TransactionCashFlowEntity c
                ON t.id = c.transactionId
            WHERE c.cashFlowType IS NULL
                AND t.portfolio = :#{#portfolio.id}
                AND t.timestamp between :from AND :to
            """)
    Collection<TransactionEntity> findByPortfolioAndTimestampBetweenDepositAndWithdrawalTransactions(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    @Query("""
            SELECT t FROM TransactionEntity t
            LEFT OUTER JOIN TransactionCashFlowEntity c
                ON t.id = c.transactionId
            WHERE c.cashFlowType IS NULL
                AND t.portfolio = :portfolio
                AND t.security.id = :security
                AND t.timestamp between :from AND :to
            """)
    Collection<TransactionEntity> findByPortfolioAndSecurityIdAndTimestampBetweenDepositAndWithdrawalTransactions(
            @Param("portfolio") String portfolio,
            @Param("security") int security,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    int countByPortfolioIn(Set<String> portfolio);
}
