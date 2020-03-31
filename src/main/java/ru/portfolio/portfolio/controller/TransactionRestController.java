/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
