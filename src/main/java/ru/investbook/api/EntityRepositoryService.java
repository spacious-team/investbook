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

package ru.investbook.api;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EntityRepositoryService<ID, Pojo> {

    @Nullable
    ID getId(Pojo object);

    boolean existsById(ID id);

    Optional<Pojo> getById(ID id);

    Page<Pojo> getPage(Pageable pageable);

    /**
     * Creates a new object with direct INSERT into DB (without prior SELECT call) if possible,
     * calls {@link #createIfAbsent(Object)} otherwise.
     *
     * @return true if object is created, false if object with ID already exists
     * @throws RuntimeException if object not exists and an INSERT error occurs
     */
    boolean insert(Pojo object);

    /**
     * Creates new object, doesn't update.
     * Calls SELECT to check if object's ID exists in DB.
     *
     * @return true if object was created, false if object with ID already exists
     * @throws RuntimeException if object not exists and an INSERT error occurs
     * @see #insert(Object)
     */
    boolean createIfAbsent(Pojo object);

    /**
     * Creates new object, doesn't update.
     * Calls SELECT to check if object's ID exists in DB.
     * Use faster {@link #createIfAbsent(Object)} method if saved object is not required
     *
     * @return created object or empty Optional if object with ID already exists
     * @throws RuntimeException if object not exists and an INSERT error occurs
     * @see #createIfAbsent(Object)
     */
    Optional<Pojo> createAndGetIfAbsent(Pojo object);

    /**
     * Creates new object, doesn't update.
     * Calls SELECT to check if object's ID exists in DB.
     * Use faster {@link #createIfAbsent(Object)} method if saved object is not required
     *
     * @return created object or existing object without update if object with ID already exists
     * @throws RuntimeException if object not exists and an INSERT error occurs
     * @see #createIfAbsent(Object)
     */
    CreateResult<Pojo> createIfAbsentAndGet(Pojo object);

    /**
     * Create new or update existing object in DB.
     * Use instead of the slower method {@link #createOrUpdateAndGet(Object)}
     * if saved object is not required
     *
     * @throws RuntimeException if underlying DB error occurs
     */
    void createOrUpdate(Pojo object);

    /**
     * Create new or update existing object in DB.
     *
     * @return the saved object (some fields, such as ID, may be set by the DBMS)
     * @throws RuntimeException if underlying DB error occurs
     * @see #createOrUpdate(Object)
     */
    Pojo createOrUpdateAndGet(Pojo object);

    void deleteById(ID id);
}
