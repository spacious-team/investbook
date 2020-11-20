/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntityPK;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/transaction-cash-flows")
public class TransactionCashFlowRestController extends AbstractRestController<TransactionCashFlowEntityPK, TransactionCashFlow, TransactionCashFlowEntity> {
    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public TransactionCashFlowRestController(TransactionCashFlowRepository repository,
                                             TransactionCashFlowConverter converter) {
        super(repository, converter);
        this.transactionCashFlowRepository = repository;
    }

    @GetMapping
    @Override
    protected List<TransactionCashFlowEntity> get() {
        return super.get();
    }

    @GetMapping("/portfolio/{portfolio}/id/{transaction-id}")
    protected List<TransactionCashFlowEntity> get(@PathVariable("portfolio") String portfolio,
                                                  @PathVariable("transaction-id") String transactionId) {
        return transactionCashFlowRepository.findByPkPortfolioAndPkTransactionId(portfolio, transactionId);
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @GetMapping("/portfolio/{portfolio}/id/{transaction-id}/events/{event-type}")
    public ResponseEntity<TransactionCashFlowEntity> get(@PathVariable("portfolio") String portfolio,
                                                         @PathVariable("transaction-id") String transactionId,
                                                         @PathVariable("event-type") int eventType) {
        return super.get(getId(portfolio, transactionId, eventType));
    }

    @PostMapping
    @Override
    public ResponseEntity<TransactionCashFlowEntity> post(@RequestBody TransactionCashFlow object) {
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @PutMapping("/portfolio/{portfolio}/id/{transaction-id}/events/{event-type}")
    public ResponseEntity<TransactionCashFlowEntity> put(@PathVariable("portfolio") String portfolio,
                                                         @PathVariable("transaction-id") String transactionId,
                                                         @PathVariable("event-type") int eventType,
                                                         @RequestBody TransactionCashFlow object) {
        return super.put(getId(portfolio, transactionId, eventType), object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @DeleteMapping("/portfolio/{portfolio}/id/{transaction-id}/events/{event-type}")
    public void delete(@PathVariable("portfolio") String portfolio,
                       @PathVariable("transaction-id") String transactionId,
                       @PathVariable("event-type") int eventType) {
        super.delete(getId(portfolio, transactionId, eventType));
    }

    @Override
    protected Optional<TransactionCashFlowEntity> getById(TransactionCashFlowEntityPK id) {
        return repository.findById(id);
    }

    @Override
    protected TransactionCashFlowEntityPK getId(TransactionCashFlow object) {
        return getId(object.getPortfolio(), object.getTransactionId(), object.getEventType().getId());
    }

    private TransactionCashFlowEntityPK getId(String portfolio, String transactionId, int eventType) {
        TransactionCashFlowEntityPK pk = new TransactionCashFlowEntityPK();
        pk.setTransactionId(transactionId);
        pk.setPortfolio(portfolio);
        pk.setType(eventType);
        return pk;
    }

    @Override
    protected TransactionCashFlow updateId(TransactionCashFlowEntityPK id, TransactionCashFlow object) {
        return object.toBuilder()
                .transactionId(id.getTransactionId())
                .portfolio(id.getPortfolio())
                .eventType(CashFlowType.valueOf(id.getType()))
                .build();
    }

    @Override
    protected URI getLocationURI(TransactionCashFlow object) throws URISyntaxException {
        return new URI(getLocation() + "/portfolio/" + object.getPortfolio()
                + "/id/"+ object.getTransactionId()
                + "/events/" + object.getEventType().getId());
    }

    @Override
    protected String getLocation() {
        return "/transaction-cash-flows";
    }
}
