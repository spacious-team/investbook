package ru.portfolio.portfolio.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EntityConverter;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class SecurityEventCashFlowRestController extends AbstractRestController<Integer, SecurityEventCashFlow, SecurityEventCashFlowEntity> {

    public SecurityEventCashFlowRestController(JpaRepository<SecurityEventCashFlowEntity, Integer> repository,
                                               EntityConverter<SecurityEventCashFlowEntity, SecurityEventCashFlow> converter) {
        super(repository, converter);
    }

    @GetMapping("/security-event-cash-flows")
    @Override
    public List<SecurityEventCashFlowEntity> get() {
        return super.get();
    }

    @GetMapping("/security-event-cash-flows/{id}")
    @Override
    public ResponseEntity<SecurityEventCashFlowEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping("/security-event-cash-flows")
    @Override
    public ResponseEntity<SecurityEventCashFlowEntity> post(@Valid @RequestBody SecurityEventCashFlow event) {
        return super.post(event);
    }

    @PutMapping("/security-event-cash-flows/{id}")
    @Override
    public ResponseEntity<SecurityEventCashFlowEntity> put(@PathVariable("id") Integer id,
                                                   @Valid @RequestBody SecurityEventCashFlow event) {
        return super.put(id, event);
    }

    @DeleteMapping("/security-event-cash-flows/{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<SecurityEventCashFlowEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(SecurityEventCashFlow object) {
        return object.getId();
    }

    @Override
    protected SecurityEventCashFlow updateId(Integer id, SecurityEventCashFlow object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/security-event-cash-flows";
    }
}
