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

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.investbook.converter.EntityConverter;

import java.util.Optional;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
import static ru.investbook.repository.RepositoryHelper.isUniqIndexViolationException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEntityRepositoryService<ID, Pojo, Entity> implements EntityRepositoryService<ID, Pojo> {
    private final JpaRepository<Entity, ID> repository;
    private final EntityConverter<Entity, Pojo> converter;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplateRequired;
    private TransactionTemplate transactionTemplateRequiresNew;

    @PostConstruct
    void init() {
        transactionTemplateRequired = new TransactionTemplate(transactionManager);
        transactionTemplateRequiresNew = new TransactionTemplate(transactionManager);
        transactionTemplateRequiresNew.setPropagationBehavior(PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean existsById(ID id) {
        return repository.existsById(id);
    }

    @Override
    public Optional<Pojo> getById(ID id) {
        return repository.findById(id)
                .map(converter::fromEntity);
    }

    @Override
    public Page<Pojo> getPage(Pageable pageable) {
        return repository.findAll(pageable)
                .map(converter::fromEntity);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean insert(Pojo object) {
        if (entityManager instanceof Session hibernateSpecificSession) {
            try {
                Entity entity = converter.toEntity(object);
                transactionTemplateRequiresNew.executeWithoutResult(_ -> hibernateSpecificSession.save(entity));
                return true;
            } catch (Exception e) {
                if (isUniqIndexViolationException(e)) {
                    return false; // can't insert, object with same ID already exists in DB or unique key constraint violation
                }
                log.error("Can't INSERT by optimized deprecated Hibernate method save(): {}", object, e);
            }
        }
        Boolean result = transactionTemplateRequired.execute(_ -> create(object));
        return Boolean.TRUE.equals(result);
    }

    @Override
    @Transactional
    public boolean create(Pojo object) {
        return createInternal(object)
                .isPresent();
    }

    @Override
    @Transactional
    public Optional<Pojo> createAndGet(Pojo object) {
        return createInternal(object)
                .map(converter::fromEntity);
    }

    /**
     * Creates a new object (with SELECT check)
     *
     * @return created entity if object is created or empty Optional otherwise
     * @implSpec Should be called in transaction
     */
    private Optional<Entity> createInternal(Pojo object) {
        ID id = getId(object);
        if (id != null && existsById(id)) {
            return Optional.empty();
        }
        // Если работать не в транзакции, то следующая строка может повторно создать объект.
        // Это возможно, если объект был создан другим потоком после проверки существования строки по ID.
        // Метод должен работать в транзакции.
        Entity savedEntity = createOrUpdateInternal(object);
        return Optional.of(savedEntity);
    }

    @Override
    @Transactional
    public void createOrUpdate(Pojo object) {
        createOrUpdateInternal(object);
    }

    @Override
    @Transactional
    public Pojo createOrUpdateAndGet(Pojo object) {
        Entity savedEntity = createOrUpdateInternal(object);
        return converter.fromEntity(savedEntity);
    }

    private Entity createOrUpdateInternal(Pojo object) {
        Entity entity = converter.toEntity(object);
        return repository.save(entity);
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
    }
}
