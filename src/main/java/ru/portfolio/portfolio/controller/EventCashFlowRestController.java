package ru.portfolio.portfolio.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EntityConverter;
import ru.portfolio.portfolio.entity.EventCashFlowEntity;
import ru.portfolio.portfolio.pojo.EventCashFlow;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class EventCashFlowRestController extends AbstractRestController<Integer, EventCashFlow, EventCashFlowEntity> {

    public EventCashFlowRestController(JpaRepository<EventCashFlowEntity, Integer> repository,
                                       EntityConverter<EventCashFlowEntity, EventCashFlow> converter) {
        super(repository, converter);
    }

    @GetMapping("/event-cash-flows")
    @Override
    public List<EventCashFlowEntity> get() {
        return super.get();
    }

    @GetMapping("/event-cash-flows/{id}")
    @Override
    public ResponseEntity<EventCashFlowEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping("/event-cash-flows")
    @Override
    public ResponseEntity<EventCashFlowEntity> post(@Valid @RequestBody EventCashFlow event) {
        return super.post(event);
    }

    @PutMapping("/event-cash-flows/{id}")
    @Override
    public ResponseEntity<EventCashFlowEntity> put(@PathVariable("id") Integer id,
                                                   @Valid @RequestBody EventCashFlow event) {
        return super.put(id, event);
    }

    @DeleteMapping("/event-cash-flows/{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<EventCashFlowEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(EventCashFlow object) {
        return object.getId();
    }

    @Override
    protected EventCashFlow updateId(Integer id, EventCashFlow object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/event-cash-flows";
    }
}
