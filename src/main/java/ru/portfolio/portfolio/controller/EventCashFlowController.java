package ru.portfolio.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EventCashFlowEntityConverter;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.repository.EventCashFlowRepository;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class EventCashFlowController {
    private final EventCashFlowRepository eventCashFlowRepository;
    private final EventCashFlowEntityConverter eventCashFlowEntityConverter;

    @GetMapping("/event-cash-flows")
    public List<EventCashFlowEntity> getIssuers() {
        return eventCashFlowRepository.findAll();
    }

    /**
     * Get the entity.
     * If entity not exists NOT_FOUND http status will be retuned.
     */
    @GetMapping("/event-cash-flows/{id}")
    public ResponseEntity<EventCashFlowEntity> getIssuerByInn(@PathVariable("id") int id) {
        return eventCashFlowRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Create a new entity.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * If entiry already exists CONFLICT http status and Location header was returned.
     * @param event new entity
     */
    @PostMapping("/event-cash-flows")
    public ResponseEntity<EventCashFlowEntity> postIssuer(@Valid @RequestBody EventCashFlow event) throws URISyntaxException {
        if (event.getId() == null) {
            return createEntity(event);
        }
        Optional<EventCashFlowEntity> result = eventCashFlowRepository.findById(event.getId());
        if (result.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .location(getLocation(event))
                    .build();
        } else {
            return createEntity(event);
        }
    }

    /**
     * Update or create a new entity.
     * In update case method returns OK http status and updated version of entity in body.
     * In create case  method returns CREATE http status, Location header and updated version of entity in body.
     * @param id updating or creating entity
     * @param event new version of entity
     * @throws URISyntaxException
     */
    @PutMapping("/event-cash-flows/{id}")
    public ResponseEntity<EventCashFlowEntity> putEntity(@PathVariable("id") int id,
                                                         @Valid @RequestBody EventCashFlow event) throws URISyntaxException {
        event = (event.getId() != null) ? event : event.toBuilder().id(id).build();
        Optional<EventCashFlowEntity> result = eventCashFlowRepository.findById(id);
        if (result.isPresent()) {
            return ResponseEntity.ok(saveAndFlush(event));
        } else {
            return createEntity(event);
        }
    }

    private EventCashFlowEntity saveAndFlush(EventCashFlow event) {
        return eventCashFlowRepository.saveAndFlush(eventCashFlowEntityConverter.toEntity(event));
    }

    /**
     * @return response entity with http CREATE status, Location http header and body
     */
    private ResponseEntity<EventCashFlowEntity> createEntity(EventCashFlow event) throws URISyntaxException {
        return ResponseEntity
                .created(getLocation(event))
                .body(saveAndFlush(event));
    }

    private URI getLocation(EventCashFlow event) throws URISyntaxException {
        return new URI("/event-cash-flows/" + event.getId());
    }

    @DeleteMapping("/event-cash-flows/{id}")
    public void delete(@PathVariable("id") int id) {
        eventCashFlowRepository.findById(id)
                .ifPresent(eventCashFlowRepository::delete);
    }
}
