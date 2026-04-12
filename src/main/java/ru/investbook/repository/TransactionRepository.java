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

import org.spacious_team.broker.pojo.Account;
import org.spacious_team.broker.pojo.Security;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.ListQuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.TransactionEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public interface TransactionRepository extends
        JpaRepository<TransactionEntity, Integer>,
        JpaSpecificationExecutor<TransactionEntity>,
        ConstraintAwareRepository<TransactionEntity, Integer>,
        ListQuerydslPredicateExecutor<TransactionEntity> {

    Optional<TransactionEntity> findFirstByOrderByTimestampAsc();

    Optional<TransactionEntity> findFirstByOrderByTimestampDesc();

    Optional<TransactionEntity> findFirstBySecurityIdOrderByTimestampDesc(Integer securityId);

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
                AND t1.account IN (:accounts)
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctSecurityByAccountInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("accounts") Collection<String> accounts,
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
                AND t.account IN (:accounts)
                AND t.timestamp between :from AND :to
            ORDER BY t.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctDerivativeByAccountInAndTimestampBetweenOrderByTimestampDesc(
            @Param("accounts") Collection<String> accounts,
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
                AND account IN (:accounts)
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByAccountInAndTimestampBetweenOrderByTimestampDesc(
            @Param("accounts") Collection<String> accounts,
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
                AND account IN (:accounts)
                AND s.ticker LIKE CONCAT(:currencyPair, '\\_%')
                AND timestamp between :from AND :to
            ORDER BY timestamp DESC
            """)
    List<Integer> findDistinctFxContractByAccountInAndCurrencyPairAndTimestampBetween(
            @Param("accounts") Collection<String> accounts,
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
                AND t1.account IN (:accounts)
                AND t2.currency = :currency
                AND timestamp between :from AND :to
            ORDER BY t1.timestamp DESC
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<Integer> findDistinctFxContractByAccountInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("accounts") Collection<String> accounts,
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

    List<TransactionEntity> findBySecurityIdAndAccountInAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
            Integer securityId,
            Collection<String> account,
            Instant fromDate,
            Instant toDate);

    List<TransactionEntity> findBySecurityIdAndTimestampBetweenOrderByTimestampAscTradeIdAsc(
            Integer securityId,
            Instant fromDate,
            Instant toDate);

    /**
     * Return first security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndAccountAndTimestampBetweenOrderByTimestampAsc(
            Integer securityId,
            String account,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndAccountAndTimestampBetweenOrderByTimestampDesc(
            Integer securityId,
            String account,
            Instant fromDate,
            Instant toDate);

    /**
     * Return security total buy count
     */
    @Query(nativeQuery = true, value = """
            SELECT sum(count) FROM transaction
            WHERE account = :#{#account.id}
                AND security = :#{#security.id}
                AND timestamp between :from AND :to
                AND count > 0
            """)
    Long findBySecurityIdAndAccountAndTimestampBetweenBuyCount(
            @Param("security") Security security,
            @Param("account") Account account,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Return security total cell count
     */
    @Query(nativeQuery = true, value = """
            SELECT abs(sum(count)) FROM transaction
            WHERE account = :#{#account.id}
                AND security = :#{#security.id}
                AND timestamp between :from AND :to
                AND count < 0
            """)
    Long findBySecurityIdAndAccountAndTimestampBetweenCellCount(
            @Param("security") Security security,
            @Param("account") Account account,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    @Query("""
            SELECT t FROM TransactionEntity t
            LEFT OUTER JOIN TransactionCashFlowEntity c
                ON t.id = c.transactionId
            WHERE c.cashFlowType IS NULL
                AND t.account = :#{#account.id}
                AND t.timestamp between :from AND :to
            """)
    Collection<TransactionEntity> findByAccountAndTimestampBetweenDepositAndWithdrawalTransactions(
            @Param("account") Account account,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    @Query("""
            SELECT t FROM TransactionEntity t
            LEFT OUTER JOIN TransactionCashFlowEntity c
                ON t.id = c.transactionId
            WHERE c.cashFlowType IS NULL
                AND t.account = :account
                AND t.security.id = :security
                AND t.timestamp between :from AND :to
            """)
    Collection<TransactionEntity> findByAccountAndSecurityIdAndTimestampBetweenDepositAndWithdrawalTransactions(
            @Param("account") String account,
            @Param("security") int security,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    int countByAccountIn(Set<String> account);

    @Override
    default boolean exists(TransactionEntity probe) {
        return countByIdOrAccountAndTradeId(probe.getId(), probe.getAccount(), probe.getTradeId()) > 0;
    }

    long countByIdOrAccountAndTradeId(Integer id, String account, String tradeId);

    @Override
    default Optional<TransactionEntity> findBy(TransactionEntity probe) {
        return findByIdOrAccountAndTradeId(probe.getId(), probe.getAccount(), probe.getTradeId());
    }

    Optional<TransactionEntity> findByIdOrAccountAndTradeId(Integer id, String account, String tradeId);
}
