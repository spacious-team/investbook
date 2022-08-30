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
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk_;
import ru.investbook.entity.ForeignExchangeRateEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;


@RequiredArgsConstructor(staticName = "of")
public class ForeignExchangeRateSearchSpecification implements Specification<ForeignExchangeRateEntity> {
    private final String currency;
    private final LocalDate date;

    @Override
    public Predicate toPredicate(Root<ForeignExchangeRateEntity> root, CriteriaQuery<?> query,
                                 CriteriaBuilder builder) {
        return Stream.of(
                        getCurrencyPredicate(root, builder),
                        getDatePredicate(root, builder)
                )
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private Predicate getCurrencyPredicate(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (hasText(currency)) {
            predicate = builder.like(
                    root.get(ForeignExchangeRateEntity_.pk).get(ForeignExchangeRateEntityPk_.CURRENCY_PAIR),
                    currency + "%"
            );
        }
        return predicate;
    }

    private Predicate getDatePredicate(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        Predicate predicate = null;
        if (date != null) {
            predicate = builder.equal(
                    root.get(ForeignExchangeRateEntity_.pk).get(ForeignExchangeRateEntityPk_.DATE),
                    date
            );
        }
        return predicate;
    }
}
