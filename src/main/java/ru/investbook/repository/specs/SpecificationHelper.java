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

import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.entity.PortfolioEntity_;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static lombok.AccessLevel.PRIVATE;
import static org.springframework.util.StringUtils.hasText;

@NoArgsConstructor(access = PRIVATE)
class SpecificationHelper {

    static Predicate filterSecurity(CriteriaBuilder builder, Path<SecurityEntity> security, String searchingSecurity) {
        return builder.or(
                builder.equal(security.get(SecurityEntity_.ticker), searchingSecurity),
                builder.equal(security.get(SecurityEntity_.isin), searchingSecurity),
                builder.like(security.get(SecurityEntity_.name), searchingSecurity + "%"));
    }

    @Nullable
    static <T> Predicate filterByDateFrom(Root<T> root,
                                          CriteriaBuilder builder,
                                          SingularAttribute<T, Instant> attribute,
                                          LocalDate dateFrom) {
        if (dateFrom == null) {
            return null;
        }
        Instant startOfDay = dateFrom.atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        return builder.greaterThanOrEqualTo(
                root.get(attribute),
                startOfDay);
    }

    @Nullable
    static <T> Predicate filterByDateTo(Root<T> root,
                                        CriteriaBuilder builder,
                                        SingularAttribute<T, Instant> attribute,
                                        LocalDate dateTo) {
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

    static <T> Predicate filterByPortfolio(Root<T> root,
                                           CriteriaBuilder builder,
                                           SingularAttribute<T, PortfolioEntity> attribute,
                                           String portfolio) {
        if (hasText(portfolio)) {
            Path<Object> path = root.get(attribute)
                    .get(PortfolioEntity_.ID);
            return builder.equal(path, portfolio);
        }
        Path<Boolean> path = root.get(attribute)
                .get(PortfolioEntity_.enabled);
        return builder.isTrue(path);
    }
}
