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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;
import ru.investbook.converter.EntityConverter;

import java.net.URI;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class AbstractRestController<ID, Pojo, Entity> extends AbstractEntityRepositoryService<ID, Pojo, Entity> {

    protected AbstractRestController(JpaRepository<Entity, ID> repository, EntityConverter<Entity, Pojo> converter) {
        super(repository, converter);
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
            return createAndGet(object)
                    .map(this::createResponseWithLocationHeader)
                    .orElseGet(() -> ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .location(getLocationURI(object))
                            .build());
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    /**
     * Updates or creates a new entity.
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
            if (create(object)) {
                return ResponseEntity.ok().build();  // todo no content
            } else {
                Pojo savedObject = createOrUpdateAndGet(object);
                return createResponseWithLocationHeader(savedObject); // todo change http status?
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    /**
     * Deletes object from storage. Always return OK http status with empty body.
     */
    // TODO should impl 404 status? https://stackoverflow.com/questions/4088350/is-rest-delete-really-idempotent
    public void delete(ID id) {
        deleteById(id);
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
