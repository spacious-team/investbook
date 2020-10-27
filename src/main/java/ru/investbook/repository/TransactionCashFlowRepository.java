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
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntityPK;
import ru.investbook.pojo.CashFlowType;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TransactionCashFlowRepository extends JpaRepository<TransactionCashFlowEntity, TransactionCashFlowEntityPK> {

    List<TransactionCashFlowEntity> findByPkPortfolioAndPkTransactionId(String portfolio,
                                                                        long transactionId);

    Optional<TransactionCashFlowEntity> findByPkPortfolioAndPkTransactionIdAndPkType(String portfolio,
                                                                                     long transactionId,
                                                                                     int cashFlowType);

    @Query(value = "SELECT distinct t.currency FROM TransactionCashFlowEntity t " +
            "WHERE t.pk.portfolio = :portfolio AND t.pk.type = :#{#cashFlowType.id}")
    List<String> findDistinctCurrencyByPkPortfolioAndPkType(String portfolio, CashFlowType cashFlowType);

    @Query(value = "SELECT distinct t.currency FROM TransactionCashFlowEntity t " +
            "WHERE t.pk.portfolio = :portfolio AND t.pk.type in (:#{#cashFlowTypes})")
    List<String> findDistinctCurrencyByPkPortfolioAndPkTypeIn(String portfolio, Set<Integer> cashFlowTypes);
}
