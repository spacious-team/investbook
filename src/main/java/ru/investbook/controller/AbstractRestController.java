/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.investbook.converter.EntityConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractRestController<ID, Pojo, Entity> {
    protected final JpaRepository<Entity, ID> repository;
    protected final EntityConverter<Entity, Pojo> converter;

    protected List<Pojo> get() {
        return repository.findAll()
                .stream()
                .map(converter::fromEntity)
                .collect(Collectors.toList());
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
     * In create case  method returns CREATE http status, Location header and entity in body.
     * If entity already exists CONFLICT http status and Location header was returned.
     * @param object new entity (ID may not be provided if it AUTOINCREMENT)
     */
    protected ResponseEntity<Void> post(Pojo object) {
        try {
            ID id = getId(object);
            if (id == null) {
                return createEntity(object);
            }
            Optional<Entity> result = getById(id);
            if (result.isPresent()) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .location(getLocationURI(object))
                        .build();
            } else {
                return createEntity(object);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    protected abstract ID getId(Pojo object);

    /**
     * Update or create a new entity.
     * In update case method returns OK http status and updated version of entity in body.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * @param id     updating or creating entity
     * @param object new version of entity
     * @throws URISyntaxException
     */
    public ResponseEntity<Void> put(ID id, Pojo object) {
        try {
            object = (getId(object) != null) ? object : updateId(id, object);
            if (!getId(object).equals(id)) {
                throw new BadRequestException("Идентификатор объекта, переданный в URI [" + id + "] и в теле " +
                        "запроса [" + getId(object) + "] не совпадают");
            }
            Optional<Entity> result = getById(id);
            if (result.isPresent()) {
                saveAndFlush(object);
                return ResponseEntity.ok().build();
            } else {
                return createEntity(object);
            }
        } catch (Exception e) {
            throw new InternalServerErrorException("Не могу создать объект", e);
        }
    }

    protected abstract Pojo updateId(ID id, Pojo object);

    private Entity saveAndFlush(Pojo object) {
        return repository.saveAndFlush(converter.toEntity(object));
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<Void> createEntity(Pojo object) throws URISyntaxException {
        Entity entity = saveAndFlush(object);
        return ResponseEntity
                .created(getLocationURI(converter.fromEntity(entity)))
                .build();
    }

    protected URI getLocationURI(Pojo object) throws URISyntaxException {
        return new URI(getLocation() + "/" + getId(object));
    }

    protected abstract String getLocation();

    /**
     * Delete object from storage. Always return OK http status with empty body.
     */
    public void delete(ID id) {
        getById(id).ifPresent(repository::delete);
    }
}
