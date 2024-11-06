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
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Issuer;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
@Tag(name = "Эмитенты", description = "Информация об эмитентах")
@RequestMapping("/api/v1/issuers")
public class IssuerRestController extends AbstractRestController<Integer, Issuer, IssuerEntity> {

    public IssuerRestController(JpaRepository<IssuerEntity, Integer> repository, EntityConverter<IssuerEntity, Issuer> converter) {
        super(repository, converter);
    }

    @Override
    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить всех",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public Page<Issuer> get(@Parameter(hidden = true)
                            Pageable pageable) {
        return super.get(pageable);
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одного", description = "Отобразить информацию об эмитенте по его номеру",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Issuer> get(@PathVariable("id")
                                      @Parameter(description = "Внутренний идентификатор эмитента")
                                      Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", responses = {
            @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
            @ApiResponse(responseCode = "409"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> post(@RequestBody @Valid Issuer issuer) {
        return super.post(issuer);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить сведения", responses = {
            @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор эмитента")
                                    Integer id,
                                    @RequestBody
                                    @Valid
                                    Issuer issuer) {
        return super.put(id, issuer);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удаляет сведения об эмитенте из БД", responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> delete(@PathVariable("id")
                                       @Parameter(description = "Внутренний идентификатор эмитента")
                                       Integer id) {
        return super.delete(id);
    }

    @Override
    public @Nullable Integer getId(Issuer object) {
        return object.getId();
    }

    @Override
    protected Issuer updateId(Integer id, Issuer object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/issuers";
    }
}
