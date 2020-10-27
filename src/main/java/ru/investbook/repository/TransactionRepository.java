/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntityPK;
import ru.investbook.pojo.Portfolio;
import ru.investbook.pojo.Security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TransactionRepository extends JpaRepository<TransactionEntity, TransactionEntityPK> {

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) = 12 " +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns stock market share and bonds ISINs
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT isin FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND t1.portfolio = :#{#portfolio.id} " +
            "AND length(isin) = 12 " +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "AND isin NOT LIKE '%_TOM' AND isin NOT LIKE '%_TOD'" +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctDerivativeByPortfolioAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM format)
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "AND (isin LIKE '%_TOM' OR isin LIKE '%_TOD')" +
            "AND timestamp between :from AND :to " +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctFxInstrumentByPortfolioAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default Collection<String> findDistinctFxCurrencyPairsAndTimestampBetween(Portfolio portfolio, Instant fromDate, Instant toDate) {
        return findDistinctFxInstrumentByPortfolioAndTimestampBetweenOrderByTimestampDesc(portfolio, fromDate, toDate)
                .stream()
                .map(e -> e.replace("_TOD", "")
                        .replace("_TOM", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM format)
     */
    @Query(nativeQuery = true, value = "SELECT DISTINCT isin FROM transaction as t1 " +
            "JOIN transaction_cash_flow as t2 " +
            "ON t1.id = t2.transaction_id " +
            "AND t1.portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "AND (isin LIKE '%_TOM' OR isin LIKE '%_TOD')" +
            "AND t2.type = 1 " +
            "AND t2.currency = :currency " +
            "AND timestamp between :from AND :to " +
            "ORDER BY t1.timestamp DESC")
    Collection<String> findDistinctFxInstrumentByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(
            @Param("portfolio") Portfolio portfolio,
            @Param("currency") String currency,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default Collection<String> findDistinctFxCurrencyPairsAndTimestampBetween(Portfolio portfolio, String currency, Instant fromDate, Instant toDate) {
        return findDistinctFxInstrumentByPortfolioAndCurrencyAndTimestampBetweenOrderByTimestampDesc(portfolio, currency, fromDate, toDate)
                .stream()
                .map(e -> e.replace("_TOD", "")
                        .replace("_TOM", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    ArrayList<TransactionEntity> findBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampAscPkIdAsc(
            String isin,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return first security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampAsc(
            String isin,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return last security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIsinAndPkPortfolioAndTimestampBetweenOrderByTimestampDesc(
            String isin,
            String portfolio,
            Instant fromDate,
            Instant toDate);

    /**
     * Return security total buy count
     */
    @Query(nativeQuery = true, value = "SELECT sum(count) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND isin = :#{#security.isin} " +
            "AND timestamp between :from AND :to " +
            "AND count > 0")
    Long findBySecurityIsinAndPkPortfolioAndTimestampBetweenBuyCount(
            @Param("security") Security security,
            @Param("portfolio") Portfolio portfolio,
            @Param("from") Instant fromDate,
            @Param("to") Instant toDate);

    /**
     * Return security total cell count
     */
    @Query(nativeQuery = true, value = "SELECT abs(sum(count)) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND isin = :#{#security.isin} " +
            "AND timestamp between :from AND :to " +
            "AND count < 0")
    Long findBySecurityIsinAndPkPortfolioAndTimestampBetweenCellCount(
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
