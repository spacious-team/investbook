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
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.entity.PortfolioPropertyEntity_;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static ru.investbook.repository.specs.SpecificationHelper.*;


@RequiredArgsConstructor(staticName = "of")
public class PortfolioPropertySearchSpecification implements Specification<PortfolioPropertyEntity> {
    private final String portfolio;
    private final LocalDate date;
    private final PortfolioPropertyType property;

    @Override
    public Predicate toPredicate(Root<PortfolioPropertyEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        String propertyName = (property == null) ? null : property.name();
        return Stream.of(
                        filterByPortfolio(root, builder, PortfolioPropertyEntity_.portfolio, portfolio),
                        filterByInstantBelongsToDate(root, builder, PortfolioPropertyEntity_.timestamp, date),
                        filterByEquals(root, builder, PortfolioPropertyEntity_.property, propertyName))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }
}
