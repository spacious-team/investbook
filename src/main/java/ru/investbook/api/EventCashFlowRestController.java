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
import org.spacious_team.broker.pojo.EventCashFlow;
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
import ru.investbook.entity.EventCashFlowEntity;

import java.util.List;
import java.util.Optional;

@RestController
@Tag(name = "Движения ДС по счету", description = """
        Ввод, вывод ДС, налоги, комиссии, а также дивиденды, купоны, амортизации по бумагам другого счета
        """)
@RequestMapping("/api/v1/event-cash-flows")
public class EventCashFlowRestController extends AbstractRestController<Integer, EventCashFlow, EventCashFlowEntity> {

    public EventCashFlowRestController(JpaRepository<EventCashFlowEntity, Integer> repository,
                                       EntityConverter<EventCashFlowEntity, EventCashFlow> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping
    @Operation(summary = "Отобразить все", description = "Отображает все выплаты по всем счетам")
    public List<EventCashFlow> get() {
        return super.get();
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одну", description = "Отобразить выплату по ее номеру")
    public ResponseEntity<EventCashFlow> get(@PathVariable("id")
                                             @Parameter(description = "Номер события")
                                                     Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Сохранить информацию в БД")
    public ResponseEntity<Void> post(@Valid @RequestBody EventCashFlow event) {
        return super.post(event);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Изменить", description = "Модифицировать информацию в БД")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Номер события")
                                            Integer id,
                                    @Valid @RequestBody EventCashFlow event) {
        return super.put(id, event);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удалить информацию из БД")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Номер события")
                               Integer id) {
        super.delete(id);
    }

    @Override
    protected Optional<EventCashFlowEntity> getById(Integer id) {
        return repository.findById(id);
    }

    @Override
    protected Integer getId(EventCashFlow object) {
        return object.getId();
    }

    @Override
    protected EventCashFlow updateId(Integer id, EventCashFlow object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/event-cash-flows";
    }
}
