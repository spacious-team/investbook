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
import org.springframework.data.jpa.domain.Specification;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk_;
import ru.investbook.entity.ForeignExchangeRateEntity_;

import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Stream;

import static org.springframework.util.StringUtils.hasText;


@RequiredArgsConstructor(staticName = "of")
public class ForeignExchangeRateSearchSpecification implements Specification<ForeignExchangeRateEntity> {
    private final @Nullable String currency;
    private final @Nullable LocalDate date;

    @Override
    public Predicate toPredicate(Root<ForeignExchangeRateEntity> root, @Nullable CriteriaQuery<?> query, CriteriaBuilder builder) {
        return Stream.of(
                        filterByCurrency(root, builder),
                        filterByDate(root, builder))
                .filter(Objects::nonNull)
                .reduce(builder::and)
                .orElseGet(builder::conjunction);
    }

    private @Nullable Predicate filterByCurrency(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        if (hasText(currency)) {
            Path<String> path = root.get(ForeignExchangeRateEntity_.pk)
                    .get(ForeignExchangeRateEntityPk_.CURRENCY_PAIR);
            return builder.like(builder.lower(path), "%" + currency.toLowerCase() + "%");
        }
        return null;
    }

    private @Nullable Predicate filterByDate(Root<ForeignExchangeRateEntity> root, CriteriaBuilder builder) {
        if (date == null) {
            return null;
        }
        Path<LocalDate> path = root.get(ForeignExchangeRateEntity_.pk)
                .get(ForeignExchangeRateEntityPk_.DATE);
        return builder.equal(path, date);
    }
}
