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
import org.springframework.lang.Nullable;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioEntity_;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;
import static ru.investbook.repository.specs.SpecificationHelper.filterSecurity;


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
                        getDateToPredicate(root, builder))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getPortfolioPredicate(Root<TransactionEntity> root,
                                            CriteriaQuery<?> query,
                                            CriteriaBuilder builder) {
        Path<String> transactionPortfolioPath = root.get(TransactionEntity_.portfolio);
        if (hasText(portfolio)) {
            return builder.equal(transactionPortfolioPath, portfolio);
        }
        //We do subquery because TransactionEntity is not related to PortfolioEntity in Java model, so
        //we can not do join query in Criteria API
        Subquery<String> subquery = query.subquery(String.class);
        Root<PortfolioEntity> portfolios = subquery.from(PortfolioEntity.class);

        Path<String> portfolioId = portfolios.get(PortfolioEntity_.ID);
        Path<Boolean> portfolioEnabled = portfolios.get(PortfolioEntity_.enabled);

        Subquery<String> enabledPortfolioIds = subquery.select(portfolioId)
                        .where(builder.isTrue(portfolioEnabled));

        return builder.in(transactionPortfolioPath)
                .value(enabledPortfolioIds);
    }

    @Nullable
    private Predicate getDateFromPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        if (dateFrom == null) {
            return null;
        }
        Instant startOfDay = dateFrom.atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        return builder.greaterThanOrEqualTo(
                root.get(TransactionEntity_.timestamp),
                startOfDay);
    }

    @Nullable
    private Predicate getDateToPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        if (dateTo == null) {
            return null;
        }
        Instant endOfDay = dateTo.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return builder.lessThanOrEqualTo(
                root.get(TransactionEntity_.timestamp),
                endOfDay);
    }

    @Nullable
    private Predicate getSecurityPredicate(Root<TransactionEntity> root, CriteriaBuilder builder) {
        if (hasText(security)) {
            Path<SecurityEntity> securityPath = root.get(TransactionEntity_.security);
            return filterSecurity(builder, securityPath, security);
        }
        return null;
    }
}
