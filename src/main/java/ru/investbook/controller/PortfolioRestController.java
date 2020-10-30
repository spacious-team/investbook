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

import org.spacious_team.broker.pojo.Portfolio;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.repository.PortfolioRepository;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class PortfolioRestController extends AbstractRestController<String, Portfolio, PortfolioEntity> {
    private final PortfolioRepository repository;

    public PortfolioRestController(PortfolioRepository repository, PortfolioConverter converter) {
        super(repository, converter);
        this.repository = repository;
    }

    @GetMapping("/portfolios")
    @Override
    public List<PortfolioEntity> get() {
        return super.get();
    }

    @GetMapping("/portfolios/{id}")
    @Override
    public ResponseEntity<PortfolioEntity> get(@PathVariable("id") String id) {
        return super.get(id);
    }

    @PostMapping("/portfolios")
    @Override
    public ResponseEntity<PortfolioEntity> post(@Valid @RequestBody Portfolio object) {
        return super.post(object);
    }

    @PutMapping("/portfolios/{id}")
    @Override
    public ResponseEntity<PortfolioEntity> put(@PathVariable("id") String id,
                                                  @Valid @RequestBody Portfolio object) {
        return super.put(id, object);
    }

    @DeleteMapping("/portfolios/{id}")
    @Override
    public void delete(@PathVariable("id") String id) {
        super.delete(id);
    }

    @Override
    protected Optional<PortfolioEntity> getById(String id) {
        return repository.findById(id);
    }

    @Override
    protected String getId(Portfolio object) {
        return object.getId();
    }

    @Override
    protected Portfolio updateId(String id, Portfolio object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/portfolios";
    }
}
