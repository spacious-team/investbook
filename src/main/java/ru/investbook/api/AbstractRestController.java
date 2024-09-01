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
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import ru.investbook.converter.EntityConverter;

import java.net.URI;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;

public abstract class AbstractRestController<ID, Pojo, Entity> extends AbstractEntityRepositoryService<ID, Pojo, Entity> {

    protected AbstractRestController(JpaRepository<Entity, ID> repository, EntityConverter<Entity, Pojo> converter) {
        super(repository, converter);
    }

    public Page<Pojo> get(Pageable pageable) {
        return getPage(pageable);
    }

    /**
     * Gets the entity.
     * If entity not exists NOT_FOUND http status will be returned.
     */
    public ResponseEntity<Pojo> get(ID id) {
        return getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates a new entity.
     * If entity has ID and record with this ID already exists in DB, "409 Conflict" http status and optional Location header was returned.
     * Otherwise, "201 Created" http status will be returned with Location header.
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
            CreateResult<Pojo> result = createIfAbsentAndGet(object);
            Pojo savedObject = result.object();
            if (result.created()) {
                return createResponseWithLocationHeader(savedObject);
            } else {
                return createConflictResponse(savedObject);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    @NonNull
    private ResponseEntity<Void> createConflictResponse(Pojo object) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CONFLICT);
        if (getId(object) != null) {
            URI locationURI = getLocationURI(object);
            response.location(locationURI);
        }
        return response.build();
    }

    /**
     * Updates or creates a new entity.
     * In create case method returns "201 Created" http status and Location header.
     * In update case method returns "204 No Content" http status.
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
            ID objectId = getId(object);
            if (nonNull(objectId) && !Objects.equals(id, objectId)) {
                throw new BadRequestException("Идентификатор объекта, переданный в URI [" + id + "] и в теле " +
                        "запроса [" + objectId + "] не совпадают");
            }
            Pojo objectWithId = nonNull(objectId) ? object : updateId(id, object);
            return createAndGetIfAbsent(objectWithId)
                    .map(this::createResponseWithLocationHeader)
                    .orElseGet(() -> {
                        createOrUpdate(objectWithId);
                        return ResponseEntity.noContent().build();
                    });
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    /**
     * Deletes object from storage. Always return "204 No Content" http status with empty body.
     */
    public ResponseEntity<Void> delete(ID id) {
        deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<Void> createResponseWithLocationHeader(Pojo object) {
        URI locationURI = getLocationURI(object);
        return ResponseEntity
                .created(locationURI)
                .build();
    }

    @SneakyThrows
    protected URI getLocationURI(Pojo object) {
        return new URI(UriUtils.encodePath(getLocation() + "/" + getId(object), UTF_8));
    }

    protected abstract String getLocation();

    /**
     * Returns new object with updated ID
     */
    protected abstract Pojo updateId(ID id, Pojo object);
}
