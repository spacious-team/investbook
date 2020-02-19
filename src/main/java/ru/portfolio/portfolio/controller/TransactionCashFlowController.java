package ru.portfolio.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.portfolio.portfolio.converter.TransactionCashFlowEntityConverter;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntity;
import ru.portfolio.portfolio.entity.TransactionCashFlowEntityPK;
import ru.portfolio.portfolio.pojo.CashFlowEvent;
import ru.portfolio.portfolio.pojo.TransactionCashFlow;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
public class TransactionCashFlowController extends AbstractController<TransactionCashFlowEntityPK, TransactionCashFlow, TransactionCashFlowEntity> {
    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public TransactionCashFlowController(TransactionCashFlowRepository repository,
                                         TransactionCashFlowEntityConverter converter) {
        super(repository, converter);
        this.transactionCashFlowRepository = repository;
    }

    @GetMapping("/transaction-cash-flows")
    @Override
    protected List<TransactionCashFlowEntity> get() {
        return super.get();
    }

    @GetMapping("/transaction-cash-flows/{transaction-id}")
    protected List<TransactionCashFlowEntity> get(@PathVariable("transaction-id") int transactionId) {
        return transactionCashFlowRepository.findByTransactionId(transactionId);
    }

    /**
     * see {@link AbstractController#get(Object)}
     */
    @GetMapping("/transaction-cash-flows/{transaction-id}/events/{event-type}")
    public ResponseEntity<TransactionCashFlowEntity> get(@PathVariable("transaction-id") int transactionId,
                                                         @PathVariable("event-type") int eventType) {
        return super.get(getId(transactionId, eventType));
    }

    @PostMapping("/transaction-cash-flows")
    @Override
    protected ResponseEntity<TransactionCashFlowEntity> post(@RequestBody TransactionCashFlow object) throws URISyntaxException {
        return super.post(object);
    }

    /**
     * see {@link AbstractController#put(Object, Object)}
     */
    @PutMapping("/transaction-cash-flows/{transaction-id}/events/{event-type}")
    public ResponseEntity<TransactionCashFlowEntity> put(@PathVariable("transaction-id") int transactionId,
                                                         @PathVariable("event-type") int eventType,
                                                         @RequestBody TransactionCashFlow object) throws URISyntaxException {
        return super.put(getId(transactionId, eventType), object);
    }

    /**
     * see {@link AbstractController#delete(Object)}
     */
    @DeleteMapping("/transaction-cash-flows/{transaction-id}/events/{event-type}")
    public void delete(@PathVariable("transaction-id") int transactionId, @PathVariable("event-type") int eventType) {
        super.delete(getId(transactionId, eventType));
    }

    @Override
    protected Optional<TransactionCashFlowEntity> getById(TransactionCashFlowEntityPK id) {
        return repository.findById(id);
    }

    @Override
    protected TransactionCashFlowEntityPK getId(TransactionCashFlow object) {
        return getId(object.getTransactionId(), object.getEventType().getType());
    }

    private TransactionCashFlowEntityPK getId(int transactionId, int eventType) {
        TransactionCashFlowEntityPK pk = new TransactionCashFlowEntityPK();
        pk.setTransactionId(transactionId);
        pk.setType(eventType);
        return pk;
    }

    @Override
    protected TransactionCashFlow updateId(TransactionCashFlowEntityPK id, TransactionCashFlow object) {
        return object.toBuilder()
                .transactionId(id.getTransactionId())
                .eventType(CashFlowEvent.valueOf(id.getType()))
                .build();
    }

    @Override
    protected URI getLocationURI(TransactionCashFlow object) throws URISyntaxException {
        return new URI(getLocation() + "/" + object.getTransactionId() + "/events/" + object.getEventType().getType());
    }

    @Override
    protected String getLocation() {
        return "/transaction-cash-flows";
    }
}
