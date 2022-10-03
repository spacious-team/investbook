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

package ru.investbook.repository.specs;

import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity_;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;


public class SecurityDepositSearchSpecification extends TransactionSearchSpecification {

    public static SecurityDepositSearchSpecification of(String portfolio,
                                                        String security,
                                                        LocalDate dateFrom,
                                                        LocalDate dateTo) {
        return new SecurityDepositSearchSpecification(portfolio, security, dateFrom, dateTo);
    }

    private SecurityDepositSearchSpecification(String portfolio, String security, LocalDate dateFrom, LocalDate dateTo) {
        super(portfolio, security, dateFrom, dateTo);
    }

    @Override
    public Predicate toPredicate(Root<TransactionEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        filterByNullPrice(root, builder, query),
                        super.toPredicate(root, query, builder))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    static Predicate filterByNullPrice(Root<TransactionEntity> transaction,
                                       CriteriaBuilder builder,
                                       CriteriaQuery<?> query) {
        // Use sub-query because TransactionEntity is not related to TransactionCashFlowEntity in Java model, so
        // can not do join query in Criteria API
        Subquery<Integer> subQuery = query.subquery(Integer.class);
        Root<TransactionCashFlowEntity> cashFlows = subQuery.from(TransactionCashFlowEntity.class);

        /*
            SELECT *
            FROM TransactionEntity t
            WHERE NOT EXISTS (
                SELECT 1
                FROM TransactionCashFlowEntity c
                WHERE
                    t.id = c.transaction_id)
         */
        return builder.not(
                builder.exists(
                        subQuery.select(builder.literal(1))
                                .where(
                                        builder.equal(
                                                transaction.get(TransactionEntity_.id),
                                                cashFlows.get(TransactionCashFlowEntity_.transactionId)))));
    }
}
