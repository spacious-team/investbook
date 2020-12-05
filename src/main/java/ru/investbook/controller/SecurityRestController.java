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

import org.spacious_team.broker.pojo.Security;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityRepository;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/securities")
public class SecurityRestController extends AbstractRestController<String, Security, SecurityEntity> {
    private final SecurityRepository repository;

    public SecurityRestController(SecurityRepository repository, SecurityConverter converter) {
        super(repository, converter);
        this.repository = repository;
    }

    @GetMapping
    @Override
    public List<SecurityEntity> get() {
        return super.get();
    }

    @GetMapping("{id}")
    @Override
    public ResponseEntity<SecurityEntity> get(@PathVariable("id") String id) {
        return super.get(id);
    }

    @PostMapping
    @Override
    public ResponseEntity<SecurityEntity> post(@Valid @RequestBody Security security) {
        return super.post(security);
    }

    @PutMapping("{id}")
    @Override
    public ResponseEntity<SecurityEntity> put(@PathVariable("id") String id, @Valid @RequestBody Security security) {
        return super.put(id, security);
    }

    @DeleteMapping("{id}")
    @Override
    public void delete(@PathVariable("id") String id) {
        super.delete(id);
    }

    @Override
    protected Optional<SecurityEntity> getById(String isin) {
        return repository.findByIsin(isin);
    }

    @Override
    protected String getId(Security object) {
        return object.getId();
    }

    @Override
    protected Security updateId(String id, Security object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/securities";
    }
}
