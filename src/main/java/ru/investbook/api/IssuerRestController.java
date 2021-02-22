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

package ru.investbook.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Эмитенты", description = "Информация об эмитентах")
@RequestMapping("/api/v1/issuers")
public class IssuerRestController extends AbstractRestController<Long, Issuer, IssuerEntity> {

    public IssuerRestController(JpaRepository<IssuerEntity, Long> repository, EntityConverter<IssuerEntity, Issuer> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping
    @Operation(summary = "Отобразить всех")
    public List<Issuer> get() {
        return super.get();
    }

    @Override
    @GetMapping("{inn}")
    @Operation(summary = "Отобразить одного", description = "Отобразить информацию по организации по идентификатору налогоплательщика")
    public ResponseEntity<Issuer> get(@PathVariable("inn")
                                      @Parameter(description = "Идентификатор налогоплательщика, например ИНН в России")
                                              Long inn) {
        return super.get(inn);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить")
    public ResponseEntity<Void> post(@Valid @RequestBody Issuer issuer) {
        return super.post(issuer);
    }

    @Override
    @PutMapping("{inn}")
    @Operation(summary = "Обновить сведения")
    public ResponseEntity<Void> put(@PathVariable("inn")
                                    @Parameter(description = "Идентификатор налогоплательщика, например ИНН в России")
                                            Long inn,
                                    @Valid @RequestBody
                                            Issuer issuer) {
        return super.put(inn, issuer);
    }

    @Override
    @DeleteMapping("{inn}")
    @Operation(summary = "Удалить", description = "Удаляет сведения об организации из БД")
    public void delete(@PathVariable("inn")
                       @Parameter(description = "Идентификатор налогоплательщика, например ИНН в России")
                               Long inn) {
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
