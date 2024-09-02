/*
 * InvestBook
 * Copyright (C) 2024  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * JpaRepository that knows about UNIQUE KEYS
 */
@NoRepositoryBean
public interface ConstraintAwareRepository<T, ID> extends JpaRepository<T, ID> {

    /**
     * Checks entity existence by UNIQUE KEY fields, other fields is ignored
     */
    boolean exists(T probe);

    /**
     * Selects entity by UNIQUE KEY fields, other fields is ignored
     */
    Optional<T> findBy(T probe);
}
