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

package ru.investbook.entity;

import jakarta.persistence.Id;
import org.hibernate.id.Assigned;
import org.hibernate.id.IdentityGenerator;

/**
 * If Entity ID is not null, then this ID is stored to DB.
 * If Entity ID is null, then RDBMS should generate ID itself.
 * <p>
 * In other words: this generator behaves like {@link org.hibernate.id.Assigned} generator if entity's field,
 * marked by {@link Id} annotation, is not null, or like {@link org.hibernate.id.IdentityGenerator} if this field is null.
 * <p>
 * Applicable only for INSERT operations.
 */
public class AssignedOrIdentityGenerator extends BeforeOrOnExecutionGenerator {

    @SuppressWarnings("unused")
    public AssignedOrIdentityGenerator() {
        // IdentityGenerator can be replaced by SelectGenerator
        // if RDBMS doesn't support AUTO_INCREMENT/IDENTITY fields
        super(new Assigned(), new IdentityGenerator());
    }
}
