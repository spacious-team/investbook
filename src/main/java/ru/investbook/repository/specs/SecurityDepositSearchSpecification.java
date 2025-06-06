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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntity_;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntity_;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;


@RequiredArgsConstructor(access = PRIVATE)
public class SecurityDepositSearchSpecification implements Specification<TransactionEntity> {

    private final TransactionSearchSpecification specification;

    public static SecurityDepositSearchSpecification of(@Nullable String portfolio,
                                                        @Nullable String security,
                                                        @Nullable LocalDate dateFrom,
                                                        @Nullable LocalDate dateTo) {
        TransactionSearchSpecification specification =
                TransactionSearchSpecification.of(portfolio, security, dateFrom, dateTo);
        return new SecurityDepositSearchSpecification(specification);
    }

    @Override
    public Predicate toPredicate(Root<TransactionEntity> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder builder) {
        requireNonNull(query);
        return Stream.of(
                        filterByNullPrice(root, builder, query),
                        specification.toPredicate(root, query, builder))
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
