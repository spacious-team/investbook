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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.IdentityGenerator;

import java.util.Objects;

/**
 * Behaves like {@link org.hibernate.id.Assigned} generator if entity {@link Id} is not null
 * or like {@link org.hibernate.id.IdentityGenerator} otherwise.
 */
// IdentityGenerator can be replaced by SelectGenerator if DB doesn't support AUTO_INCREMENT/IDENTITY fields.
public class AssignedOrIdentityGenerator extends IdentityGenerator implements BeforeExecutionGenerator {
    static final String NAME = "AssignedOrIdentityGenerator";

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity, Object currentValue, EventType eventType) {
        return getId(entity, session);
    }

    private static Object getId(Object entity, SharedSessionContractImplementor session) {
        return session.getEntityPersister(null, entity)
                .getIdentifier(entity, session);
    }

    /**
     * @implNote Method {@link BeforeExecutionGenerator#generate(SharedSessionContractImplementor, Object, Object, EventType)}
     * is called if this method returns false
     */
    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        Object id = getId(entity, session);
        return Objects.isNull(id);
    }

    @Override
    public boolean generatedOnExecution() {
        // The choice must be made in the generatedOnExecution(entity, session)
        // support mixed-timing generators (hibernate-core:
        return true;
    }
}
