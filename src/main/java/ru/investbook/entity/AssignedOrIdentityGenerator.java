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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentityGenerator;

import java.util.Objects;
import java.util.Properties;

/**
 * If Entity ID is not null, then this ID is stored to DB.
 * If Entity ID is null, then RDBMS should generate ID itself.
 * <p>
 * In other words: this generator behaves like {@link org.hibernate.generator.Assigned} generator if entity's field,
 * marked by {@link Id} annotation, is not null, or like {@link org.hibernate.id.IdentityGenerator} if this field is null.
 * <p>
 * Applicable only for INSERT operations.
 */
public class AssignedOrIdentityGenerator extends IdentityGenerator {

    /**
     * The configuration parameter holding the entity name
     */
    private static final String ENTITY_NAME = "entity_name";
    private @Nullable String entityName = null;

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;  // разрешаем сохранение идентификаторов, установленных в Entity объектах
    }

    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        // указываем генерировать идентификатор на стороне БД, если отсутствует в Entity объекте
        @Nullable Object id = getEntityIdentifier(entity, session);
        return Objects.isNull(id);
    }

    private @Nullable Object getEntityIdentifier(Object entity, SharedSessionContractImplementor session) {
        return session.getEntityPersister(entityName, entity)
                .getIdentifier(entity, session);
    }

    @Override
    public void configure(GeneratorCreationContext creationContext, Properties parameters) {
        super.configure(creationContext, parameters);
        entityName = parameters.getProperty(ENTITY_NAME);
    }
}
