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

import org.spacious_team.broker.pojo.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.EntityConverter;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.entity.TransactionEntityPK;
import ru.investbook.view.PositionsFactory;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/transactions")
public class TransactionRestController extends AbstractRestController<TransactionEntityPK, Transaction, TransactionEntity> {
    private final PositionsFactory positionsFactory;

    public TransactionRestController(JpaRepository<TransactionEntity, TransactionEntityPK> repository,
                                     EntityConverter<TransactionEntity, Transaction> converter,
                                     PositionsFactory positionsFactory) {
        super(repository, converter);
        this.positionsFactory = positionsFactory;
    }

    @Override
    @GetMapping
    protected List<TransactionEntity> get() {
        return super.get();
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @GetMapping("/portfolios/{portfolio}/ids/{id}")
    public ResponseEntity<TransactionEntity> get(@PathVariable("portfolio") String portfolio,
                                                 @PathVariable("id") String id) {
        return super.get(getId(portfolio, id));
    }

    @Override
    @PostMapping
    public ResponseEntity<TransactionEntity> post(@Valid @RequestBody Transaction object) {
        positionsFactory.invalidateCache();
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @PutMapping("/portfolios/{portfolio}/ids/{id}")
    public ResponseEntity<TransactionEntity> put(@PathVariable("portfolio") String portfolio,
                                                 @PathVariable("id") String id,
                                                 @Valid @RequestBody Transaction object) {
        positionsFactory.invalidateCache();
        return super.put(getId(portfolio, id), object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @DeleteMapping("/portfolios/{portfolio}/ids/{id}")
    public void delete(@PathVariable("portfolio") String portfolio,
                       @PathVariable("id") String id) {
        positionsFactory.invalidateCache();
        super.delete(getId(portfolio, id));
    }

    @Override
    protected Optional<TransactionEntity> getById(TransactionEntityPK id) {
        return repository.findById(id);
    }

    @Override
    protected TransactionEntityPK getId(Transaction object) {
        return getId(object.getPortfolio(), object.getId());
    }

    private TransactionEntityPK getId(String portfolio, String transactionId) {
        TransactionEntityPK pk = new TransactionEntityPK();
        pk.setId(transactionId);
        pk.setPortfolio(portfolio);
        return pk;
    }

    @Override
    protected Transaction updateId(TransactionEntityPK id, Transaction object) {
        return object.toBuilder()
                .id(id.getId())
                .portfolio(id.getPortfolio())
                .build();
    }

    @Override
    protected URI getLocationURI(Transaction object) throws URISyntaxException {
        return new URI(getLocation() + "/portfolios/" + object.getPortfolio() + "/ids/" + object.getId());
    }

    @Override
    protected String getLocation() {
        return "/transactions";
    }
}
