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
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk_;
import ru.investbook.entity.ForeignExchangeRateEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
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
    public Predicate toPredicate(Root<ForeignExchangeRateEntity> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        filterByCurrency(root, builder),
                        filterByDate(root, builder))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    @Nullable
    private Predicate filterByCurrency(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        if (hasText(currency)) {
            Path<String> path = root.get(ForeignExchangeRateEntity_.pk)
                    .get(ForeignExchangeRateEntityPk_.CURRENCY_PAIR);
            return builder.like(path, currency + "%");
        }
        return null;
    }

    @Nullable
    private Predicate filterByDate(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        if (date == null) {
            return null;
        }
        Path<LocalDate> path = root.get(ForeignExchangeRateEntity_.pk)
                .get(ForeignExchangeRateEntityPk_.DATE);
        return builder.equal(path, date);
    }
}
