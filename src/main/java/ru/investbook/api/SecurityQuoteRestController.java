/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.SecurityQuote;
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
import ru.investbook.entity.SecurityQuoteEntity;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@Tag(name = "Котировки", description = "Котировки биржевых инструментов")
@RequestMapping("/api/v1/security-quotes")
public class SecurityQuoteRestController extends AbstractRestController<Integer, SecurityQuote, SecurityQuoteEntity> {

    public SecurityQuoteRestController(JpaRepository<SecurityQuoteEntity, Integer> repository,
                                       EntityConverter<SecurityQuoteEntity, SecurityQuote> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping
    @Operation(summary = "Отобразить все", description = "Отобразить всю историю котировок по всем инструментам")
    public List<SecurityQuote> get() {
        return super.get();
    }


    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одну", description = "Отобразить котировку по номеру записи в БД")
    public ResponseEntity<SecurityQuote> get(@PathVariable("id")
                                             @Parameter(description = "Номер записи о котировке")
                                                     Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить")
    public ResponseEntity<Void> post(@Valid @RequestBody SecurityQuote quote) {
        return super.post(quote);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Изменить")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Номер записи о котировке")
                                            Integer id,
                                    @Valid @RequestBody SecurityQuote quote) {
        return super.put(id, quote);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Номер записи о котировке")
                               Integer id) {
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
