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

import com.querydsl.core.types.Predicate;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.investbook.converter.EntityConverter;
import ru.investbook.repository.ConstraintAwareRepository;

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
    public Page<Pojo> getPage(@Nullable Predicate predicate, Pageable pageable) {
        if (predicate != null && repository instanceof QuerydslPredicateExecutor) {
            return ((QuerydslPredicateExecutor<Entity>) repository).findAll(predicate, pageable)
                    .map(converter::fromEntity);
        }
        return repository.findAll(pageable)
                .map(converter::fromEntity);
    }

    /**
     * @implNote Method performance is the same as {@link #createIfAbsent(Object)} for H2 2.2.224 and MariaDB 11.2
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean insert(Pojo object) {
        if (entityManager instanceof Session hibernateSpecificSession) {
            try {
                Entity entity = converter.toEntity(object);
                // Hibernate save() method does sql INSERT
                transactionTemplateRequiresNew.executeWithoutResult(_ -> hibernateSpecificSession.save(entity));
                return true;
            } catch (Exception e) {
                if (isUniqIndexViolationException(e)) {
                    return false; // can't insert, object with same ID already exists in DB or unique key constraint violation
                }
                log.error("Can't INSERT by optimized deprecated Hibernate method save(): {}", object, e);
            }
        }
        @Nullable Boolean result = transactionTemplateRequired.execute(_ -> createIfAbsent(object));
        return Boolean.TRUE.equals(result);
    }

    @Override
    @Transactional
    public boolean createIfAbsent(Pojo object) {
        return createIfAbsentInternal(object)
                .isPresent();
    }

    @Override
    @Transactional
    public Optional<Pojo> createAndGetIfAbsent(Pojo object) {
        return createIfAbsentInternal(object)
                .map(converter::fromEntity);
    }

    @Override
    public CreateResult<Pojo> createIfAbsentAndGet(Pojo object) {
        return createIfAbsentAndGetInternal(object);
    }

    /**
     * Creates a new object (with SELECT check)
     *
     * @return created entity if object is created or empty Optional otherwise
     * @implSpec Should be called in transaction
     */
    private Optional<Entity> createIfAbsentInternal(Pojo object) {
        @Nullable Entity entity = null;
        if (repository instanceof ConstraintAwareRepository<Entity, ID> caRepository) {
            entity = converter.toEntity(object);
            if (caRepository.exists(entity)) {
                return Optional.empty();
            }
        } else {
            @Nullable ID id = getId(object);
            if (id != null && existsById(id)) {
                return Optional.empty();
            }
        }

        // Если работать не в транзакции, то следующие строки могут повторно создать объект.
        // Это возможно, если объект был создан другим потоком после проверки существования строки.
        // Метод должен работать в транзакции.
        if (entity == null) {
            entity = converter.toEntity(object);
        }
        Entity savedEntity = repository.save(entity);
        return Optional.of(savedEntity);
    }

    /**
     * Creates a new object (with SELECT check)
     *
     * @return created entity if object is created or existing object otherwise
     * @implSpec Should be called in transaction
     */
    private CreateResult<Pojo> createIfAbsentAndGetInternal(Pojo object) {
        @Nullable Entity entity = null;
        Optional<Entity> selectedEntity;
        if (repository instanceof ConstraintAwareRepository<Entity, ID> caRepository) {
            entity = converter.toEntity(object);
            selectedEntity = caRepository.findBy(entity);
        } else {
            @Nullable ID id = getId(object);
            selectedEntity = Optional.ofNullable(id)
                    .flatMap(repository::findById);
        }
        if (selectedEntity.isPresent()) {
            return selectedEntity
                    .map(converter::fromEntity)
                    .map(CreateResult::selected)
                    .orElseThrow();
        }

        // Если работать не в транзакции, то следующие строки могут повторно создать объект.
        // Это возможно, если объект был создан другим потоком после проверки существования строки.
        // Метод должен работать в транзакции.
        if (entity == null) {
            entity = converter.toEntity(object);
        }
        Entity savedEntity = repository.save(entity);
        Pojo savedObject = converter.fromEntity(savedEntity);
        return CreateResult.created(savedObject);
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
