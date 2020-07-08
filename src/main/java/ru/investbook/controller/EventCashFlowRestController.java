/*
 * InvestBook
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

package ru.investbook.controller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.investbook.converter.EntityConverter;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.pojo.EventCashFlow;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
public class EventCashFlowRestController extends AbstractRestController<Integer, EventCashFlow, EventCashFlowEntity> {

    public EventCashFlowRestController(JpaRepository<EventCashFlowEntity, Integer> repository,
                                       EntityConverter<EventCashFlowEntity, EventCashFlow> converter) {
        super(repository, converter);
    }

    @GetMapping("/event-cash-flows")
    @Override
    public List<EventCashFlowEntity> get() {
        return super.get();
    }

    @GetMapping("/event-cash-flows/{id}")
    @Override
    public ResponseEntity<EventCashFlowEntity> get(@PathVariable("id") Integer id) {
        return super.get(id);
    }

    @PostMapping("/event-cash-flows")
    @Override
    public ResponseEntity<EventCashFlowEntity> post(@Valid @RequestBody EventCashFlow event) {
        return super.post(event);
    }

    @PutMapping("/event-cash-flows/{id}")
    @Override
    public ResponseEntity<EventCashFlowEntity> put(@PathVariable("id") Integer id,
                                                   @Valid @RequestBody EventCashFlow event) {
        return super.put(id, event);
    }

    @DeleteMapping("/event-cash-flows/{id}")
    @Override
    public void delete(@PathVariable("id") Integer id) {
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
