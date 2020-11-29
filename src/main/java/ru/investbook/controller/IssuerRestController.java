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

import org.spacious_team.broker.pojo.Issuer;
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
import ru.investbook.entity.IssuerEntity;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/issuers")
public class IssuerRestController extends AbstractRestController<Long, Issuer, IssuerEntity> {

    public IssuerRestController(JpaRepository<IssuerEntity, Long> repository, EntityConverter<IssuerEntity, Issuer> converter) {
        super(repository, converter);
    }

    @GetMapping
    @Override
    public List<IssuerEntity> get() {
        return super.get();
    }

    @GetMapping("{inn}")
    @Override
    public ResponseEntity<IssuerEntity> get(@PathVariable("inn") Long inn) {
        return super.get(inn);
    }

    @PostMapping
    @Override
    public ResponseEntity<IssuerEntity> post(@Valid @RequestBody Issuer issuer) {
        return super.post(issuer);
    }

    @PutMapping("{inn}")
    @Override
    public ResponseEntity<IssuerEntity> put(@PathVariable("inn") Long inn,
                                                  @Valid @RequestBody Issuer issuer) {
        return super.put(inn, issuer);
    }

    @DeleteMapping("{inn}")
    @Override
    public void delete(@PathVariable("inn") Long inn) {
        super.delete(inn);
    }

    @Override
    protected Optional<IssuerEntity> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    protected Long getId(Issuer object) {
        return object.getInn();
    }

    @Override
    protected Issuer updateId(Long inn, Issuer object) {
        return object.toBuilder().inn(inn).build();
    }

    @Override
    protected String getLocation() {
        return "/issuers";
    }
}
