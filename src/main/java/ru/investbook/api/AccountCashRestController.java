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

import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.AccountCash;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
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
import ru.investbook.entity.AccountCashEntity;

import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
@Tag(name = "Account Balance Information")
@RequestMapping("/api/v1/account-cash")
public class AccountCashRestController extends AbstractRestController<Integer, AccountCash, AccountCashEntity> {

    public AccountCashRestController(JpaRepository<AccountCashEntity, Integer> repository,
                                     EntityConverter<AccountCashEntity, AccountCash> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Get all", description = "Get all records",
            operationId = "getAccountCashList",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public Page<AccountCash> get(@Parameter(hidden = true)
                                 @QuerydslPredicate(root = AccountCashEntity.class)
                                 @Nullable
                                 Predicate predicate,
                                 @Parameter(hidden = true)
                                 Pageable pageable) {
        return (predicate == null) ? super.get(pageable) : super.get(predicate, pageable);
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Get one record", description = "Get record by ID",
            operationId = "getAccountCash",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<AccountCash> get(@PathVariable("id")
                                           @Parameter(description = "Internal ID")
                                           Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Create", description = "Create record",
            operationId = "postAccountCash",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
                    @ApiResponse(responseCode = "409"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> post(@RequestBody @Valid AccountCash property) {
        return super.post(property);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить", description = "Обновить информацию для счета",
            operationId = "putAccountCash",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор записи")
                                    Integer id,
                                    @RequestBody
                                    @Valid
                                    AccountCash property) {
        return super.put(id, property);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить",
            operationId = "deleteAccountCash",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> delete(@PathVariable("id")
                                       @Parameter(description = "Внутренний идентификатор записи")
                                       Integer id) {
        return super.delete(id);
    }

    @Override
    public @Nullable Integer getId(AccountCash object) {
        return object.getId();
    }

    @Override
    protected AccountCash updateId(Integer id, AccountCash object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/account-cash";
    }
}
