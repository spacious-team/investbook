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

package ru.investbook.api;

import jakarta.persistence.GeneratedValue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import ru.investbook.converter.EntityConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

@RequiredArgsConstructor
public abstract class AbstractRestController<ID, Pojo, Entity> {
    protected final JpaRepository<Entity, ID> repository;
    protected final EntityConverter<Entity, Pojo> converter;


    protected Page<Pojo> get(Pageable pageable) {
        return repository.findAll(pageable)
                .map(converter::fromEntity);
    }

    /**
     * Get the entity.
     * If entity not exists NOT_FOUND http status will be returned.
     */
    public ResponseEntity<Pojo> get(ID id) {
        return getById(id)
                .map(converter::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    protected abstract Optional<Entity> getById(ID id);

    /**
     * Create a new entity.
     * If entity has ID and record with this ID already exists in DB, CONFLICT http status and Location header was returned.
     * Otherwise, CREATE http status will be returned with Location header.
     * When creating new object, ID may be passed, but that ID only used when no {@link GeneratedValue} set
     * on Entity ID field or if {@link GeneratedValue#generator} set to {@link org.hibernate.generator.BeforeExecutionGenerator},
     * generator impl; otherwise ID, passed in object, will be ignored (see JPA impl).
     *
     * @param object new entity (ID may be missed)
     * @throws InternalServerErrorException if object not created or updated
     */
    @Transactional
    protected ResponseEntity<Void> post(Pojo object) {
        try {
            ID id = getId(object);
            if (id == null) {
                return createOrUpdateEntity(object);
            }
            if (existsById(id)) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .location(getLocationURI(object))
                        .build();
            } else {
                // Если убрать @Transactional над методом, то следующая строка может обновить объект по ошибке.
                // Это возможно, если объект был создан другим потоком после проверки существования строки по ID.
                // По этой причине @Transactional убирать не нужно.
                return createOrUpdateEntity(object);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    protected abstract ID getId(Pojo object);

    /**
     * Update or create a new entity.
     * In update case method returns OK http status.
     * In create case method returns CREATE http status and Location header.
     * When creating new object, ID may be passed in the object, but it should be same as {@code id} argument.
     *
     * @param id     updating or creating entity id
     * @param object entity
     * @throws BadRequestException          if ID provided in object not same as in argument
     * @throws InternalServerErrorException if object not created or updated
     */
    @Transactional
    public ResponseEntity<Void> put(ID id, Pojo object) {
        try {
            if (getId(object) == null) {
                object = updateId(id, object);
            } else if (!Objects.equals(id, getId(object))) {
                throw new BadRequestException("Идентификатор объекта, переданный в URI [" + id + "] и в теле " +
                        "запроса [" + getId(object) + "] не совпадают");
            }
            if (existsById(id)) {
                saveAndFlush(object);
                return ResponseEntity.ok().build();
            } else {
                return createOrUpdateEntity(object);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    private boolean existsById(ID id) {
        return repository.existsById(id);
    }

    protected abstract Pojo updateId(ID id, Pojo object);

    private Entity saveAndFlush(Pojo object) {
        Entity entity = converter.toEntity(object);
        return repository.saveAndFlush(entity);
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<Void> createOrUpdateEntity(Pojo object) throws URISyntaxException {
        Entity entity = saveAndFlush(object);
        Pojo savedObject = converter.fromEntity(entity);
        URI locationURI = getLocationURI(savedObject);
        return ResponseEntity
                .created(locationURI)
                .build();
    }

    protected URI getLocationURI(Pojo object) throws URISyntaxException {
        return new URI(UriUtils.encodePath(getLocation() + "/" + getId(object), UTF_8));
    }

    protected abstract String getLocation();

    /**
     * Delete object from storage. Always return OK http status with empty body.
     */
    public void delete(ID id) {
        repository.deleteById(id);
    }
}
