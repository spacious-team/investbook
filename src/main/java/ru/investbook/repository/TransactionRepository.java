/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntityPK;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TransactionRepository extends JpaRepository<TransactionEntity, TransactionEntityPK> {

    Optional<TransactionEntity> findFirstByOrderByTimestampAsc();

    List<TransactionEntity> findByOrderByPkPortfolioAscTimestampDescSecurityIdAsc();

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT distinct security FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(security) = 12 " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    List<String> findDistinctSecurityByPortfolioAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT security FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND t1.portfolio IN (:portfolios) " +
            "AND length(security) = 12 " +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    List<String> findDistinctSecurityByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT security FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND length(security) = 12 " +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    List<String> findDistinctSecurityByCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct security FROM transaction " +
            "WHERE portfolio IN (:portfolios) " +
            "AND length(security) <> 12 " +
            "AND security NOT LIKE '______\\_%' " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    List<String> findDistinctDerivativeByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct security FROM transaction " +
            "WHERE length(security) <> 12 " +
            "AND security NOT LIKE '______\\_%' " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    List<String> findDistinctDerivativeByTimestampBetweenOrderByTimestampDesc(
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = "SELECT distinct security FROM transaction " +
            "WHERE portfolio IN (:portfolios) " +
            "AND length(security) <> 12 " +
            "AND security LIKE '______\\_%' " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    List<String> findDistinctFxContractByPortfolioInAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = "SELECT distinct security FROM transaction " +
            "WHERE length(security) <> 12 " +
            "AND security LIKE '______\\_%' " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    List<String> findDistinctFxContractByTimestampBetweenOrderByTimestampDesc(
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default List<String> findDistinctFxCurrencyPairByPortfolioInAndTimestampBetween(
            Collection<String> portfolios,
            Instant fromDate,
            Instant toDate) {
        return findDistinctFxContractByPortfolioInAndTimestampBetweenOrderByTimestampDesc(portfolios, fromDate, toDate)
                .stream()
                .map(e -> e.substring(0, Math.min(6, e.length())))
                .distinct()
                .collect(Collectors.toList());
    }

    default List<String> findDistinctFxContractByPortfolioInAndCurrencyPairAndTimestampBetween(
            Collection<String> portfolios,
            String currencyPair,
            Instant fromDate,
            Instant toDate) {
        return findDistinctFxContractByPortfolioInAndTimestampBetweenOrderByTimestampDesc(portfolios, fromDate, toDate)
                .stream()
                .filter(contract -> contract.startsWith(currencyPair))
                .collect(Collectors.toList());
    }

    default List<String> findDistinctFxContractByCurrencyPairAndTimestampBetween(
            String currencyPair,
            Instant fromDate,
            Instant toDate) {
        return findDistinctFxContractByTimestampBetweenOrderByTimestampDesc(fromDate, toDate)
                .stream()
                .filter(contract -> contract.startsWith(currencyPair))
                .collect(Collectors.toList());
    }

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT security FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND t1.portfolio IN (:portfolios) " +
            "AND length(security) <> 12 " +
            "AND security LIKE '______\\_%' " +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    List<String> findDistinctFxContractByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolios") Collection<String> portfolios,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM, USDRUB_CNGD format)
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT security FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND length(security) <> 12 " +
            "AND security LIKE '______\\_%' " +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    List<String> findDistinctFxContractByCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default List<String> findDistinctFxCurrencyPairByPortfolioInAndCurrencyAndTimestampBetween(
            Collection<String> portfolios,
            String currency,
            Instant fromDate,
            Instant toDate) {
        return findDistinctFxContractByPortfolioInAndCurrencyAndTimestampBetweenOrderByTimestampDesc(portfolios, currency, fromDate, toDate)
                .stream()
                .map(e -> e.substring(0, Math.min(6, e.length())))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default List<String> findDistinctFxCurrencyPairByCurrencyAndTimestampBetween(
            String currency,
            Instant fromDate,
            Instant toDate) {
        return findDistinctFxContractByCurrencyAndTimestampBetweenOrderByTimestampDesc(currency, fromDate, toDate)
                .stream()
                .map(e -> e.substring(0, Math.min(6, e.length())))
                .distinct()
                .collect(Collectors.toList());
    }

    List<TransactionEntity> findBySecurityIdAndPkPortfolioInAndTimestampBetweenOrderByTimestampAscPkIdAsc(
            String isin,
            Collection<String> portfolio,
            Instant fromDate,
            Instant toDate);

    List<TransactionEntity> findBySecurityIdAndTimestampBetweenOrderByTimestampAscPkIdAsc(
            String isin,
            Instant fromDate,
            Instant toDate);

    /**
     * Return first security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndPkPortfolioAndTimestampBetweenOrderByTimestampAsc(
            String isin,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIdAndPkPortfolioAndTimestampBetweenOrderByTimestampDesc(
            String isin,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return security total buy count
     */
    @Query(nativeQuery = true, value = "SELECT sum(count) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND security = :#{#security.id} " +
            "AND timestamp between :from AND :to " +
            "AND count > 0")
    Long findBySecurityIdAndPkPortfolioAndTimestampBetweenBuyCount(
            @Param("security") Security security,
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Return security total cell count
     */
    @Query(nativeQuery = true, value = "SELECT abs(sum(count)) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND security = :#{#security.id} " +
            "AND timestamp between :from AND :to " +
            "AND count < 0")
    Long findBySecurityIdAndPkPortfolioAndTimestampBetweenCellCount(
            @Param("security") Security security,
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    @Query("SELECT t FROM TransactionEntity t " +
            "LEFT OUTER JOIN TransactionCashFlowEntity c " +
            "ON t.pk.id = c.pk.transactionId AND t.pk.portfolio = c.pk.portfolio " +
            "WHERE c.pk.type IS NULL AND t.pk.portfolio = :#{#portfolio.id} AND t.timestamp between :from AND :to")
    Collection<TransactionEntity> findByPkPortfolioAndTimestampBetweenDepositAndWithdrawalTransactions(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);
}
