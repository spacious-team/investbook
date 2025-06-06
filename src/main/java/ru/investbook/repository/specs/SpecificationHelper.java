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
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import ru.investbook.entity.CashFlowTypeEntity;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioEntity_;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEntity_;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.StringUtils.hasText;

@NoArgsConstructor(access = PRIVATE)
class SpecificationHelper {

    static <T> @Nullable Predicate filterBySecurity(Root<T> root,
                                                    CriteriaBuilder builder,
                                                    SingularAttribute<T, SecurityEntity> attribute,
                                                    @Nullable String security) {
        if (hasText(security)) {
            Path<SecurityEntity> securityPath = root.get(attribute);
            return builder.or(
                    filterByLike(builder, securityPath.get(SecurityEntity_.ticker), security),
                    filterByLike(builder, securityPath.get(SecurityEntity_.isin), security),
                    filterByLike(builder, securityPath.get(SecurityEntity_.name), security));
        }
        return null;
    }

    static <T> @Nullable Predicate filterBySecurityId(Root<T> root,
                                                      CriteriaBuilder builder,
                                                      SingularAttribute<T, Integer> attribute,
                                                      @Nullable String security,
                                                      CriteriaQuery<?> query) {
        if (hasText(security)) {
            // Do sub-query because SecurityEntity is not related to SecurityDescriptionEntity in Java model, so
            // can not do join query in Criteria API
            Subquery<Integer> subQuery = query.subquery(Integer.class);
            Root<SecurityEntity> securities = subQuery.from(SecurityEntity.class);

            Subquery<Integer> requestedSecurityIds = subQuery.select(securities.get(SecurityEntity_.id))
                    .where(builder.or(
                            filterByLike(builder, securities.get(SecurityEntity_.ticker), security),
                            filterByLike(builder, securities.get(SecurityEntity_.isin), security),
                            filterByLike(builder, securities.get(SecurityEntity_.name), security)));

            Path<Integer> securityIdPath = root.get(attribute);
            return builder.in(securityIdPath)
                    .value(requestedSecurityIds);
        }
        return null;
    }

    static <T> @Nullable Predicate filterByDateFrom(Root<T> root,
                                                    CriteriaBuilder builder,
                                                    SingularAttribute<T, Instant> attribute,
                                                    @Nullable LocalDate dateFrom) {
        if (dateFrom == null) {
            return null;
        }
        Instant startOfDay = dateFrom.atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        return builder.greaterThanOrEqualTo(
                root.get(attribute),
                startOfDay);
    }

    static <T> @Nullable Predicate filterByDateTo(Root<T> root,
                                                  CriteriaBuilder builder,
                                                  SingularAttribute<T, Instant> attribute,
                                                  @Nullable LocalDate dateTo) {
        if (dateTo == null) {
            return null;
        }
        Instant endOfDay = dateTo.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return builder.lessThanOrEqualTo(
                root.get(attribute),
                endOfDay);
    }

    static <T> @Nullable Predicate filterByInstantBelongsToDate(Root<T> root,
                                                                CriteriaBuilder builder,
                                                                SingularAttribute<T, Instant> attribute,
                                                                @Nullable LocalDate date) {
        if (date == null) {
            return null;
        }
        Instant startOfDay = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        Instant endOfDay = date.atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        return builder.between(
                root.get(attribute),
                startOfDay,
                endOfDay);
    }

    static <T> Predicate filterByPortfolio(Root<T> root,
                                           CriteriaBuilder builder,
                                           SingularAttribute<T, PortfolioEntity> attribute,
                                           @Nullable String portfolio) {
        if (hasText(portfolio)) {
            Path<String> path = root.get(attribute)
                    .get(PortfolioEntity_.ID);
            return builder.equal(path, portfolio);
        }
        Path<Boolean> path = root.get(attribute)
                .get(PortfolioEntity_.enabled);
        return builder.isTrue(path);
    }

    static <T> Predicate filterByPortfolioName(Root<T> root,
                                               CriteriaBuilder builder,
                                               SingularAttribute<T, String> attribute,
                                               @Nullable String portfolio,
                                               CriteriaQuery<?> query) {
        Path<String> transactionPortfolioPath = root.get(attribute);
        if (hasText(portfolio)) {
            return builder.equal(transactionPortfolioPath, portfolio);
        }
        // Do sub-query because <...>Entity is not related to PortfolioEntity in Java model, so
        // can not do join query in Criteria API
        Subquery<String> subQuery = query.subquery(String.class);
        Root<PortfolioEntity> portfolios = subQuery.from(PortfolioEntity.class);

        Path<String> portfolioId = portfolios.get(PortfolioEntity_.id);
        Path<Boolean> portfolioEnabled = portfolios.get(PortfolioEntity_.enabled);

        Subquery<String> enabledPortfolioIds = subQuery.select(portfolioId)
                .where(builder.isTrue(portfolioEnabled));

        return builder.in(transactionPortfolioPath)
                .value(enabledPortfolioIds);
    }

    static <X> @Nullable Predicate filterByEquals(Root<X> root,
                                                  CriteriaBuilder builder,
                                                  SingularAttribute<X, CashFlowTypeEntity> attribute,
                                                  @Nullable CashFlowType value) {
        if (nonNull(value)) {
            Path<CashFlowTypeEntity> path = root.get(attribute);
            CashFlowTypeEntity entity = new CashFlowTypeEntity();
            entity.setId(value.getId());
            return builder.equal(path, entity);
        }
        return null;
    }

    static <X, T> @Nullable Predicate filterByEquals(Root<X> root,
                                                     CriteriaBuilder builder,
                                                     SingularAttribute<X, T> attribute,
                                                     @Nullable String value) {
        if (hasText(value)) {
            Path<T> path = root.get(attribute);
            return builder.equal(path, value);
        }
        return null;
    }

    static <X> @Nullable Predicate filterByLike(Root<X> root,
                                                CriteriaBuilder builder,
                                                SingularAttribute<X, String> attribute,
                                                @Nullable String value) {
        if (hasText(value)) {
            Path<String> path = root.get(attribute);
            return filterByLike(builder, path, value);
        }
        return null;
    }

    private static Predicate filterByLike(CriteriaBuilder builder, Path<String> path, String value) {
        return builder.like(builder.lower(path), "%" + value.toLowerCase() + "%");
    }
}
