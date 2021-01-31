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

package ru.investbook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.entity.CashFlowTypeEntity;
import ru.investbook.repository.CashFlowTypeRepository;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Tag(name = "Типы событий")
@RequestMapping("/api/v1/cash-flow-types")
public class CashFlowTypeRestController {

    private final CashFlowTypeRepository cashFlowTypeRepository;

    @GetMapping
    @Operation(summary = "Отобразить все")
    public Iterable<CashFlowTypeEntity> getCashFlowType() {
        return cashFlowTypeRepository.findAll();
    }

    @GetMapping("{id}")
    @Operation(summary = "Отобразить по идентификатору")
    public ResponseEntity<CashFlowTypeEntity> getCashFlowType(@PathVariable("id")
                                                              @Parameter(description = "Идентификатор типа")
                                                                      Integer id) {
        Optional<CashFlowTypeEntity> result = cashFlowTypeRepository.findById(id);
        return result
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
