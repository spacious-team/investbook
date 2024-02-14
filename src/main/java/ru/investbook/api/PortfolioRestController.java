/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import jakarta.validation.Valid;
import org.spacious_team.broker.pojo.Portfolio;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.PortfolioEntity;
import ru.investbook.repository.PortfolioRepository;

import java.util.Optional;

@RestController
@Tag(name = "Счета")
@RequestMapping("/api/v1/portfolios")
public class PortfolioRestController extends AbstractRestController<String, Portfolio, PortfolioEntity> {
    private final PortfolioRepository repository;

    public PortfolioRestController(PortfolioRepository repository, PortfolioConverter converter) {
        super(repository, converter);
        this.repository = repository;
    }

    @Override
    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить все")
    public Page<Portfolio> get(@Parameter(hidden = true)
                                   Pageable pageable) {
        return super.get(pageable);
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить один")
    public ResponseEntity<Portfolio> get(@PathVariable("id")
                                         @Parameter(description = "Номер счета")
                                                 String id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить")
    public ResponseEntity<Void> post(@Valid @RequestBody Portfolio object) {
        return super.post(object);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Добавить")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Номер счета")
                                            String id,
                                    @Valid @RequestBody Portfolio object) {
        return super.put(id, object);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удалить счет и все связанные с ним данные")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Номер счета")
                               String id) {
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
