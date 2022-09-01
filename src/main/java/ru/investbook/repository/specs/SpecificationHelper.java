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
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEntity_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
class SpecificationHelper {

    static Predicate filterSecurity(CriteriaBuilder builder, Path<SecurityEntity> security, String searchingSecurity) {
        return builder.or(
                builder.equal(security.get(SecurityEntity_.ticker), searchingSecurity),
                builder.equal(security.get(SecurityEntity_.isin), searchingSecurity),
                builder.like(security.get(SecurityEntity_.name), searchingSecurity + "%"));
    }
}
