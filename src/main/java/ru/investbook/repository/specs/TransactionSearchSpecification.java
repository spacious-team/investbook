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

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioEntity_;
import ru.investbook.entity.SecurityEntity_;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;


@RequiredArgsConstructor(staticName = "of")
public class TransactionSearchSpecification implements Specification<TransactionEntity> {
    private final String portfolio;
    private final String security;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    @Override
    public Predicate toPredicate(Root<TransactionEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        getPortfolioPredicate(root, query, builder),
                        getSecurityPredicate(root, builder),
                        getDateFromPredicate(root, builder),
                        getDateToPredicate(root, builder)
                )
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getPortfolioPredicate(Root<TransactionEntity> root, CriteriaQuery<?> query,
                                            CriteriaBuilder builder) {
        Predicate predicate;
        if (hasText(portfolio)) {
            predicate = builder.equal(
                    root.get(TransactionEntity_.portfolio),
                    portfolio
            );
        } else {
            //We do subquery because TransactionEntity is not related to PortfolioEntity in Java model, so
            //we can not do join query in Criteria API
            Subquery<String> activePortfoliosSubquery = query.subquery(String.class);
            Root<PortfolioEntity> activePortfoliosRoot = activePortfoliosSubquery.from(PortfolioEntity.class);

            predicate = builder.in(root.get(TransactionEntity_.portfolio)).value(
                    activePortfoliosSubquery
                            .select(activePortfoliosRoot.get(PortfolioEntity_.ID))
                            .where(builder.and(builder.isTrue(activePortfoliosRoot.get(PortfolioEntity_.enabled))))
            );
        }
        return predicate;
    }

    private Predicate getDateFromPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateFrom != null) {
            predicate = builder.greaterThanOrEqualTo(
                    root.get(TransactionEntity_.timestamp),
                    dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getDateToPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateTo != null) {
            predicate = builder.lessThanOrEqualTo(
                    root.get(TransactionEntity_.timestamp),
                    dateTo.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getSecurityPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (hasText(security)) {
            predicate= builder.or(
                    builder.equal(root.get(TransactionEntity_.security).get(SecurityEntity_.ticker), security),
                    builder.equal(root.get(TransactionEntity_.security).get(SecurityEntity_.isin), security),
                    builder.like(root.get(TransactionEntity_.security).get(SecurityEntity_.name), security +"%")
            );
        }
        return predicate;
    }
}
