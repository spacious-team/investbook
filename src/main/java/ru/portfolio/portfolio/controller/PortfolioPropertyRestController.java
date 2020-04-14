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
import ru.portfolio.portfolio.entity.PortfolioPropertyEntity;
import ru.portfolio.portfolio.pojo.PortfolioProperty;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class PortfolioPropertyRestController extends AbstractRestController<Integer, PortfolioProperty, PortfolioPropertyEntity> {

    public PortfolioPropertyRestController(JpaRepository<PortfolioPropertyEntity, Integer> repository,
                                           EntityConverter<PortfolioPropertyEntity, PortfolioProperty> converter) {
        super(repository, converter);
    }

    @GetMapping("/portfolio-properties")
    @Override
    public List<PortfolioPropertyEntity> get() {
        return super.get();
    }

    @GetMapping("/portfolio-properties/{id}")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping("/portfolio-properties")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> post(@Valid @RequestBody PortfolioProperty property) {
        return super.post(property);
    }

    @PutMapping("/portfolio-properties/{id}")
    @Override
    public ResponseEntity<PortfolioPropertyEntity> put(@PathVariable("id") Integer id,
                                                        @Valid @RequestBody PortfolioProperty property) {
        return super.put(id, property);
    }

    @DeleteMapping("/portfolio-properties/{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<PortfolioPropertyEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(PortfolioProperty object) {
        return object.getId();
    }

    @Override
    protected PortfolioProperty updateId(Integer id, PortfolioProperty object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/portfolio-properties";
    }
}
