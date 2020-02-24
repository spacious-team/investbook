package ru.portfolio.portfolio.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.EntityConverter;
import ru.portfolio.portfolio.entity.TransactionEntity;
import ru.portfolio.portfolio.pojo.Transaction;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class TransactionRestController extends AbstractRestController<Long, Transaction, TransactionEntity> {

    public TransactionRestController(JpaRepository<TransactionEntity, Long> repository,
                                     EntityConverter<TransactionEntity, Transaction> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping("/transactions")
    protected List<TransactionEntity> get() {
        return super.get();
    }

    @Override
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionEntity> get(@PathVariable("id") Long id) {
        return super.get(id);
    }

    @Override
    @PostMapping("/transactions")
    public ResponseEntity<TransactionEntity> post(@Valid @RequestBody Transaction object) {
        return super.post(object);
    }

    @Override
    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionEntity> put(@PathVariable("id") Long id,
                                                 @Valid @RequestBody Transaction object) {
        return super.put(id, object);
    }

    @Override
    @DeleteMapping("/transactions/{id}")
    public void delete(@PathVariable("id") Long id) {
        super.delete(id);
    }

    @Override
    protected Optional<TransactionEntity> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    protected Long getId(Transaction object) {
        return object.getId();
    }

    @Override
    protected Transaction updateId(Long id, Transaction object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/transactions";
    }
}
