/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.entity.TransactionCashFlowEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public interface TransactionCashFlowRepository extends JpaRepository<TransactionCashFlowEntity, Integer> {

    default boolean isDepositOrWithdrawal(int transactionId) {
        return countByTransactionId(transactionId) == 0;
    }

    int countByTransactionId(int transactionId);

    List<TransactionCashFlowEntity> findByTransactionId(int transactionId);

    @Query(nativeQuery = true, value = """
            SELECT * FROM transaction_cash_flow
            WHERE transaction_id = :transactionId AND type = :#{#cashFlowType.id}
            """)
    Optional<TransactionCashFlowEntity> findByTransactionIdAndCashFlowType(int transactionId,
                                                                           CashFlowType cashFlowType);

    @Query(nativeQuery = true, value = """
            SELECT * FROM transaction_cash_flow
            WHERE transaction_id = :transactionId AND type in (:#{#cashFlowTypes})
            """)
    List<TransactionCashFlowEntity> findByTransactionIdAndCashFlowTypeIn(int transactionId,
                                                                         Set<Integer> cashFlowTypes);

    @Query(nativeQuery = true, value = """
            SELECT distinct c.currency
            FROM transaction t JOIN transaction_cash_flow c
                ON t.id = c.transaction_id
            WHERE t.portfolio = :portfolio AND c.type = :#{#cashFlowType.id}
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<String> findDistinctCurrencyByPortfolioAndCashFlowType(String portfolio, CashFlowType cashFlowType);

    @Query(nativeQuery = true, value = """
            SELECT distinct c.currency
            FROM transaction t JOIN transaction_cash_flow c
                ON t.id = c.transaction_id
            WHERE t.portfolio IN (:portfolios) AND c.type in (:#{#cashFlowTypes})
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<String> findDistinctCurrencyByPortfolioInAndCashFlowTypeIn(Collection<String> portfolios,
                                                                    Set<Integer> cashFlowTypes);

    @Query(nativeQuery = true, value = """
            SELECT distinct currency FROM transaction_cash_flow
            WHERE type in (:#{#cashFlowTypes})
            """)
    @SuppressWarnings("SpringDataRepositoryMethodReturnTypeInspection")
    List<String> findDistinctCurrencyByCashFlowTypeIn(Set<Integer> cashFlowTypes);

    @Transactional
    void deleteByTransactionId(int transactionId);
}
