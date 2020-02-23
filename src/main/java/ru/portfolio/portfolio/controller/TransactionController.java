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
public class TransactionController extends AbstractController<Integer, Transaction, TransactionEntity> {

    public TransactionController(JpaRepository<TransactionEntity, Integer> repository,
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
    public ResponseEntity<TransactionEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping("/transactions")
    protected ResponseEntity<TransactionEntity> post(@Valid @RequestBody Transaction object) {
        return super.post(object);
    }

    @Override
    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionEntity> put(@PathVariable("id") Integer id,
                                                 @Valid @RequestBody Transaction object) {
        return super.put(id, object);
    }

    @Override
    @DeleteMapping("/transactions/{id}")
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<TransactionEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(Transaction object) {
        return object.getId();
    }

    @Override
    protected Transaction updateId(Integer id, Transaction object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/transactions";
    }
}
