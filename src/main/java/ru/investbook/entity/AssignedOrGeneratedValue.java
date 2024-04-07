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

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Provides ID generation strategy for the values of primary keys.
 * The annotation may be applied to a primary key property with the {@link Id} annotation.
 * Behaves like {@link GeneratedValue} if primary key is null or use assigned value otherwise.
 * <p/>
 * Annotation helps to replace code snippet
 * <pre>
 *     &#64;Id
 *     &#64;GeneratedValue(generator = "generator-name")
 *     &#64;GenericGenerator(name = "generator-name", type = AssignedOrIdentityGenerator.class)
 *     Integer id;
 * </pre>
 * with shorter one
 * <pre>
 *     &#64;Id
 *     &#64;AssignedOrGeneratedValue
 *     Integer id;
 * </pre>
 * </p>
 *
 * @see jakarta.persistence.GeneratedValue
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD})
@IdGeneratorType(AssignedOrIdentityGenerator.class)
public @interface AssignedOrGeneratedValue {
}
