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
import ru.investbook.entity.SecurityEntity_;
import ru.investbook.entity.SecurityQuoteEntity;
import ru.investbook.entity.SecurityQuoteEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.isNotBlank;


@RequiredArgsConstructor(staticName = "of")
public class SecurityQuoteSearchSpecification implements Specification<SecurityQuoteEntity> {
    private final String security;
    private final String currency;
    private final LocalDate date;

    @Override
    public Predicate toPredicate(Root<SecurityQuoteEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        getSecurityPredicate(root, builder),
                        getCurrencyPredicate(root, builder),
                        getDatePredicate(root, builder)
                )
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getCurrencyPredicate(Root<SecurityQuoteEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (StringUtils.isNotBlank(currency)) {
            predicate = builder.equal(
                    root.get(SecurityQuoteEntity_.currency),
                    currency
            );
        }
        return predicate;
    }

    private Predicate getDatePredicate(Root<SecurityQuoteEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (date != null) {
            predicate = builder.between(
                    root.get(SecurityQuoteEntity_.timestamp),
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
        }
        return predicate;
    }

    private Predicate getSecurityPredicate(Root<SecurityQuoteEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (isNotBlank(security)) {
            predicate= builder.or(
                    builder.equal(root.get(SecurityQuoteEntity_.security).get(SecurityEntity_.ticker), security),
                    builder.equal(root.get(SecurityQuoteEntity_.security).get(SecurityEntity_.isin), security),
                    builder.like(root.get(SecurityQuoteEntity_.security).get(SecurityEntity_.name), security +"%")
            );
        }
        return predicate;
    }
}
