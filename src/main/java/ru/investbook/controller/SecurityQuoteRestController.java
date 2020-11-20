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

import org.spacious_team.broker.pojo.SecurityQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.investbook.converter.EntityConverter;
import ru.investbook.entity.SecurityQuoteEntity;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/security-quotes")
public class SecurityQuoteRestController extends AbstractRestController<Integer, SecurityQuote, SecurityQuoteEntity> {

    public SecurityQuoteRestController(JpaRepository<SecurityQuoteEntity, Integer> repository,
                                       EntityConverter<SecurityQuoteEntity, SecurityQuote> converter) {
        super(repository, converter);
    }

    @GetMapping
    @Override
    public List<SecurityQuoteEntity> get() {
        return super.get();
    }

    @GetMapping("{id}")
    @Override
    public ResponseEntity<SecurityQuoteEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping
    @Override
    public ResponseEntity<SecurityQuoteEntity> post(@Valid @RequestBody SecurityQuote quote) {
        return super.post(quote);
    }

    @PutMapping("{id}")
    @Override
    public ResponseEntity<SecurityQuoteEntity> put(@PathVariable("id") Integer id,
                                                   @Valid @RequestBody SecurityQuote quote) {
        return super.put(id, quote);
    }

    @DeleteMapping("{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<SecurityQuoteEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(SecurityQuote object) {
        return object.getId();
    }

    @Override
    protected SecurityQuote updateId(Integer id, SecurityQuote object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/security-quotes";
    }
}
