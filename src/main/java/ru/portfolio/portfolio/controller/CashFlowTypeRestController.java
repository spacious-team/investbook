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

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.entity.CashFlowTypeEntity;
import ru.portfolio.portfolio.repository.CashFlowTypeRepository;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class CashFlowTypeRestController {

    private final CashFlowTypeRepository cashFlowTypeRepository;

    @GetMapping("/cash-flow-types")
    public Iterable<CashFlowTypeEntity> getCashFlowType() {
        return cashFlowTypeRepository.findAll();
    }

    @GetMapping("/cash-flow-types/{id}")
    public ResponseEntity<CashFlowTypeEntity> getCashFlowType(@PathVariable("id") Integer id) {
        Optional<CashFlowTypeEntity> result = cashFlowTypeRepository.findById(id);
        return result
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
