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
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.EventCashFlowEntity_;
import ru.investbook.entity.PortfolioEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;
import static ru.investbook.repository.specs.SpecificationHelper.filterByDateFrom;
import static ru.investbook.repository.specs.SpecificationHelper.filterByDateTo;


@RequiredArgsConstructor(staticName = "of")
public class EventCashFlowEntitySearchSpecification implements Specification<EventCashFlowEntity> {
    private final String portfolio;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    @Override
    public Predicate toPredicate(Root<EventCashFlowEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        getPortfolioPredicate(root, builder),
                        filterByDateFrom(root, builder, EventCashFlowEntity_.timestamp, dateFrom),
                        filterByDateTo(root, builder, EventCashFlowEntity_.timestamp, dateTo))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getPortfolioPredicate(Root<EventCashFlowEntity> root, CriteriaBuilder builder) {
        if (hasText(portfolio)) {
            Path<Object> path = root.get(EventCashFlowEntity_.portfolio)
                    .get(PortfolioEntity_.ID);
            return builder.equal(path, portfolio);
        }
        Path<Boolean> path = root.get(EventCashFlowEntity_.portfolio)
                .get(PortfolioEntity_.enabled);
        return builder.isTrue(path);
    }
}
