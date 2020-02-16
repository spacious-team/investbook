package ru.portfolio.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.portfolio.portfolio.converter.EntityConverter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public abstract class AbstractController<ID, Pojo, Entity> {
    protected final JpaRepository<Entity, ID> repository;
    private final EntityConverter<Entity, Pojo> converter;

    protected List<Entity> get() {
        return repository.findAll();
    }

    /**
     * Get the entity.
     * If entity not exists NOT_FOUND http status will be retuned.
     */
    public ResponseEntity<Entity> get(ID id) {
        return getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    protected abstract Optional<Entity> getById(ID id);

    /**
     * Create a new entity.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * If entiry already exists CONFLICT http status and Location header was returned.
     * @param object new entity
     */
    protected ResponseEntity<Entity> post(Pojo object) throws URISyntaxException {
        if (getId(object) == null) {
            return createEntity(object);
        }
        Optional<Entity> result = getById(getId(object));
        if (result.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .location(getLocationURI(object))
                    .build();
        } else {
            return createEntity(object);
        }
    }

    protected abstract ID getId(Pojo object);

    /**
     * Update or create a new entity.
     * In update case method returns OK http status and updated version of entity in body.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * @param id updating or creating entity
     * @param object new version of entity
     * @throws URISyntaxException
     */
    public ResponseEntity<Entity> put(ID id, Pojo object) throws URISyntaxException {
        object = (getId(object) != null) ? object : updateId(id, object);
        if (!getId(object).equals(id)) {
            throw new BadRequestException("Идентификатор объекта, переданный в URI [" + id + "] и в теле " +
                    "запроса [" + getId(object) + "] не совпадают");
        }
        Optional<Entity> result = getById(id);
        if (result.isPresent()) {
            return ResponseEntity.ok(saveAndFlush(object));
        } else {
            return createEntity(object);
        }
    }

    protected abstract Pojo updateId(ID id, Pojo object);

    private Entity saveAndFlush(Pojo object) {
        return repository.saveAndFlush(converter.toEntity(object));
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<Entity> createEntity(Pojo object) throws URISyntaxException {
        return ResponseEntity
                .created(getLocationURI(object))
                .body(saveAndFlush(object));
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
