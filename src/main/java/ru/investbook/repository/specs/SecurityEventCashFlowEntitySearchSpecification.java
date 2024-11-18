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
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.CashFlowTypeEntity_;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity_;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static ru.investbook.repository.specs.SpecificationHelper.*;


@RequiredArgsConstructor(staticName = "of")
public class SecurityEventCashFlowEntitySearchSpecification implements Specification<SecurityEventCashFlowEntity> {
    private final @Nullable String portfolio;
    private final @Nullable String security;
    private final @Nullable LocalDate dateFrom;
    private final @Nullable LocalDate dateTo;
    private final @Nullable CashFlowType cashFlowType;

    @Override
    public Predicate toPredicate(Root<SecurityEventCashFlowEntity> root,
                                 @Nullable CriteriaQuery<?> query,
                                 CriteriaBuilder builder) {
        return Stream.of(
                        filterByPortfolio(root, builder, SecurityEventCashFlowEntity_.portfolio, portfolio),
                        filterByDateFrom(root, builder, SecurityEventCashFlowEntity_.timestamp, dateFrom),
                        filterByDateTo(root, builder, SecurityEventCashFlowEntity_.timestamp, dateTo),
                        filterBySecurity(root, builder, SecurityEventCashFlowEntity_.security, security),
                        filterByEquals(root, builder, SecurityEventCashFlowEntity_.cashFlowType, cashFlowType),
                        filterByCacheFlowType(root, builder))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate filterByCacheFlowType(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        Path<Object> path = root.get(SecurityEventCashFlowEntity_.cashFlowType)
                .get(CashFlowTypeEntity_.ID);
        return builder.notEqual(path, CashFlowType.TAX.getId());
    }
}
