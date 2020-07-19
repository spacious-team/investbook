/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

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
            "ORDER BY t1.timestamp DESC")
    Collection<String> findDistinctIsinByPortfolioAndCurrencyOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio,
                                                                                  @Param("currency") String currency);

    /**
     * Returns derivatives market contracts
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "AND isin NOT LIKE '%_TOM' AND isin NOT LIKE '%_TOD'" +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctDerivativeByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    /**
     * Returns foreign exchange market contracts (in USDRUB_TOD, USDRUB_TOM format)
     */
    @Query(nativeQuery = true, value = "SELECT distinct isin FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND length(isin) <> 12 " +
            "AND (isin LIKE '%_TOM' OR isin LIKE '%_TOD')" +
            "ORDER BY timestamp DESC")
    Collection<String> findDistinctFxInstrumentByPortfolioOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default Collection<String> findDistinctFxCurrencyPairs(Portfolio portfolio) {
        return findDistinctFxInstrumentByPortfolioOrderByTimestampDesc(portfolio)
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
            "ORDER BY t1.timestamp DESC")
    Collection<String> findDistinctFxInstrumentByPortfolioAndCurrencyOrderByTimestampDesc(@Param("portfolio") Portfolio portfolio,
                                                                                          @Param("currency") String currency);

    /**
     * Returns foreign exchange market currency pairs (in USDRUB format)
     */
    default Collection<String> findDistinctFxCurrencyPairs(Portfolio portfolio, String currency) {
        return findDistinctFxInstrumentByPortfolioAndCurrencyOrderByTimestampDesc(portfolio, currency)
                .stream()
                .map(e -> e.replace("_TOD", "")
                        .replace("_TOM", ""))
                .distinct()
                .collect(Collectors.toList());
    }

    ArrayList<TransactionEntity> findBySecurityIsinAndPkPortfolioOrderByTimestampAscPkIdAsc(String isin,
                                                                                            String portfolio);

    /**
     * Return first security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIsinAndPkPortfolioOrderByTimestampAsc(String isin, String portfolio);

    /**
     * Return last security transaction
     */
    Optional<TransactionEntity> findFirstBySecurityIsinAndPkPortfolioOrderByTimestampDesc(String isin, String portfolio);

    /**
     * Return security total buy count
     */
    @Query(nativeQuery = true, value = "SELECT sum(count) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND isin = :#{#security.isin} " +
            "AND count > 0")
    Long findBySecurityIsinAndPkPortfolioBuyCount(@Param("security") Security security,
                                                  @Param("portfolio") Portfolio portfolio);

    /**
     * Return security total cell count
     */
    @Query(nativeQuery = true, value = "SELECT abs(sum(count)) FROM transaction " +
            "WHERE portfolio = :#{#portfolio.id} " +
            "AND isin = :#{#security.isin} " +
            "AND count < 0")
    Long findBySecurityIsinAndPkPortfolioCellCount(@Param("security") Security security,
                                                   @Param("portfolio") Portfolio portfolio);

    @Query("SELECT t FROM TransactionEntity t " +
            "LEFT OUTER JOIN TransactionCashFlowEntity c " +
            "ON t.pk.id = c.pk.transactionId AND t.pk.portfolio = c.pk.portfolio AND c.pk.type = 1 " +
            "WHERE c.pk.type IS NULL AND t.pk.portfolio = :#{#portfolio.id}")
    Collection<TransactionEntity> findDepositAndWithdrawalTransactions(@Param("portfolio") Portfolio portfolio);
}
