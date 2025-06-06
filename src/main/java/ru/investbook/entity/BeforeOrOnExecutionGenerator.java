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

import lombok.Getter;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.persister.entity.EntityPersister;

import java.util.EnumSet;

import static java.util.Objects.isNull;


/**
 * Allows to determine at runtime (based on Entity for example) whether an ID should be generated on client or on DB server side.
 * To implement this constructor accepts {@link BeforeExecutionGenerator} and {@link OnExecutionGenerator} ID generators.
 * <p>
 * If java {@link BeforeExecutionGenerator#generate(SharedSessionContractImplementor, Object entity, Object, EventType)}
 * returns not null ID, when this ID will be stored to DB. In another case, the DBMS must generate ID itself
 * based on the strategy implemented by {@link OnExecutionGenerator}.
 */
@SuppressWarnings("removal")
class BeforeOrOnExecutionGenerator implements BeforeExecutionGenerator, OnExecutionGenerator {

    private final BeforeExecutionGenerator beforeExecutionGenerator;
    private final OnExecutionGenerator onExecutionGenerator;
    @Getter
    private final EnumSet<EventType> eventTypes;

    public BeforeOrOnExecutionGenerator(BeforeExecutionGenerator beforeExecutionGenerator,
                                        OnExecutionGenerator onExecutionGenerator) {
        this.beforeExecutionGenerator = beforeExecutionGenerator;
        this.onExecutionGenerator = onExecutionGenerator;
        this.eventTypes = EnumSet.copyOf(beforeExecutionGenerator.getEventTypes());
        eventTypes.addAll(onExecutionGenerator.getEventTypes());
    }

    @Override
    public boolean generatedOnExecution() {
        // This method is called to configure a context (using this Generator) without knowledge of a specific Entity.
        // The choice for the real Entity must be made in the this.generatedOnExecution(entity, session) method.
        // For example, find out comment "support mixed-timing generators" in IdentifierGeneratorUtil.class (hibernate-core):
        // true is required, if this Generator want sometimes generate ID by RDBMS (for example by AUTO_INCREMENT)
        return true;
    }

    /**
     * @implNote If this method returns false,
     * then {@link #generate(SharedSessionContractImplementor, Object, Object, EventType)} is called to get ID
     */
    @Override
    public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
        try {
            EventType eventType = beforeExecutionGenerator.getEventTypes().iterator().next();
            @SuppressWarnings("argument")
            Object id = beforeExecutionGenerator.generate(session, entity, null, eventType);
            return isNull(id);
        } catch (Exception e) {
            return true;  // RDBMS should generate ID
        }
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object entity, Object currentValue, EventType eventType) {
        return beforeExecutionGenerator.generate(session, entity, currentValue, eventType);
    }

    @Override
    public boolean referenceColumnsInSql(Dialect dialect) {
        return onExecutionGenerator.referenceColumnsInSql(dialect);
    }

    @Override
    public boolean writePropertyValue() {
        return onExecutionGenerator.writePropertyValue();
    }

    @Override
    public String[] getReferencedColumnValues(Dialect dialect) {
        return onExecutionGenerator.getReferencedColumnValues(dialect);
    }

    @Override
    public InsertGeneratedIdentifierDelegate getGeneratedIdentifierDelegate(PostInsertIdentityPersister persister) {
        return onExecutionGenerator.getGeneratedIdentifierDelegate(persister);
    }

    @Override
    public String[] getUniqueKeyPropertyNames(EntityPersister persister) {
        return onExecutionGenerator.getUniqueKeyPropertyNames(persister);
    }
}
