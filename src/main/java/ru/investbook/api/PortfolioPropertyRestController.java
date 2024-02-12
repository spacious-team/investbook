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
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.EntityConverter;
import ru.investbook.entity.PortfolioPropertyEntity;

import java.util.Optional;

@RestController
@Tag(name = "Информация по счетам")
@RequestMapping("/api/v1/portfolio-properties")
public class PortfolioPropertyRestController extends AbstractRestController<Integer, PortfolioProperty, PortfolioPropertyEntity> {

    public PortfolioPropertyRestController(JpaRepository<PortfolioPropertyEntity, Integer> repository,
                                           EntityConverter<PortfolioPropertyEntity, PortfolioProperty> converter) {
        super(repository, converter);
    }

    @GetMapping
    @Operation(summary = "Отобразить все", description = "Отображает всю имеющуюся информацию обо всех счетах")
    public Page<PortfolioProperty> get(@RequestParam(value = "page", defaultValue = ApiUtil.DEFAULT_PAGE, required = false)
                                           @Parameter(description = "Номер страницы")
                                           int pageNo,
                                       @RequestParam(value = "size", defaultValue = ApiUtil.DEFAULT_PAGE_SIZE, required = false)
                                           @Parameter(description = "Количество записей на странице")
                                           int pageSize,
                                       @RequestParam(value = "sortBy", defaultValue = ApiUtil.DEFAULT_PORTFOLIO_PROPERTY_SORT_BY, required = false)
                                           @Parameter(description = "Атрибут сортировки")
                                           String sortBy,
                                       @RequestParam(value = "sortDir", defaultValue = ApiUtil.DEFAULT_SORT_DIRECTION, required = false)
                                           @Parameter(description = "Направление сортировки")
                                           String sortDir) {
        return super.get(ApiUtil.getPage(pageNo, pageSize, sortBy, sortDir));
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить один", description = "Отображает информацию по идентификатору")
    public ResponseEntity<PortfolioProperty> get(@PathVariable("id")
                                                 @Parameter(description = "Внутренний идентификатор записи")
                                                         Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Добавить информацию для конкретного счета")
    public ResponseEntity<Void> post(@Valid @RequestBody PortfolioProperty property) {
        return super.post(property);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить", description = "Обновить информацию для счета")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор записи")
                                            Integer id,
                                    @Valid @RequestBody PortfolioProperty property) {
        return super.put(id, property);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Внутренний идентификатор записи")
                               Integer id) {
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
