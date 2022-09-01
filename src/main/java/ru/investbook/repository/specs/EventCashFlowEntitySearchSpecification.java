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
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.EventCashFlowEntity_;
import ru.investbook.entity.PortfolioEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;


@RequiredArgsConstructor(staticName = "of")
public class EventCashFlowEntitySearchSpecification implements Specification<EventCashFlowEntity> {
    private final String portfolio;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    @Override
    public Predicate toPredicate(Root<EventCashFlowEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        return Stream.of(
                        getPortfolioPredicate(root, builder),
                        getDateFromPredicate(root, builder),
                        getDateToPredicate(root, builder)
                )
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getPortfolioPredicate(Root<EventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate;
        if (StringUtils.isNotBlank(portfolio)) {
            predicate = builder.equal(
                    root.get(EventCashFlowEntity_.portfolio).get(PortfolioEntity_.ID),
                    portfolio
            );
        } else {
            predicate = builder.isTrue(root.get(EventCashFlowEntity_.portfolio).get(PortfolioEntity_.enabled));
        }
        return predicate;
    }

    private Predicate getDateFromPredicate(Root<EventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateFrom != null) {
            predicate = builder.greaterThanOrEqualTo(
                    root.get(EventCashFlowEntity_.timestamp),
                    dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getDateToPredicate(Root<EventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateTo != null) {
            predicate = builder.lessThanOrEqualTo(
                    root.get(EventCashFlowEntity_.timestamp),
                    dateTo.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }
}
