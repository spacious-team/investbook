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
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.CashFlowTypeEntity_;
import ru.investbook.entity.PortfolioEntity_;
import ru.investbook.entity.SecurityEntity_;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;


@RequiredArgsConstructor(staticName = "of")
public class SecurityEventCashFlowEntitySearchSpecification implements Specification<SecurityEventCashFlowEntity> {
    private final String portfolio;
    private final String security;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;

    @Override
    public Predicate toPredicate(Root<SecurityEventCashFlowEntity> root, CriteriaQuery<?> query,
                                 CriteriaBuilder builder) {
        return Stream.of(
                        getPortfolioPredicate(root, builder),
                        getDateFromPredicate(root, builder),
                        getDateToPredicate(root, builder),
                        getSecurityPredicate(root, builder),
                        getCacheFlowTypePredicate(root, builder)
                )
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getPortfolioPredicate(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate;
        if (hasText(portfolio)) {
            predicate = builder.equal(
                    root.get(SecurityEventCashFlowEntity_.portfolio).get(PortfolioEntity_.ID),
                    portfolio
            );
        } else {
            predicate = builder.isTrue(root.get(SecurityEventCashFlowEntity_.portfolio).get(PortfolioEntity_.enabled));
        }
        return predicate;
    }

    private Predicate getDateFromPredicate(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateFrom != null) {
            predicate = builder.greaterThanOrEqualTo(
                    root.get(SecurityEventCashFlowEntity_.timestamp),
                    dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getDateToPredicate(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (dateTo != null) {
            predicate = builder.lessThanOrEqualTo(
                    root.get(SecurityEventCashFlowEntity_.timestamp),
                    dateTo.atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getSecurityPredicate(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (hasText(security)) {
            predicate= builder.or(
                    builder.equal(root.get(SecurityEventCashFlowEntity_.security).get(SecurityEntity_.ticker), security),
                    builder.equal(root.get(SecurityEventCashFlowEntity_.security).get(SecurityEntity_.isin), security),
                    builder.like(root.get(SecurityEventCashFlowEntity_.security).get(SecurityEntity_.name), security +"%")
            );
        }
        return predicate;
    }

    private Predicate getCacheFlowTypePredicate(Root<SecurityEventCashFlowEntity> root, CriteriaBuilder builder) {
        return builder.notEqual(
                root.get(SecurityEventCashFlowEntity_.cashFlowType).get(CashFlowTypeEntity_.ID),
                CashFlowType.TAX.getId()
        );
    }
}
