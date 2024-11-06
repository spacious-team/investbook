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
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
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
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.report.FifoPositionsFactory;

import static org.spacious_team.broker.pojo.CashFlowType.REDEMPTION;
import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
@Tag(name = "События по бумаге", description = "Дивиденды, купоны, амортизации, вариационная маржа, комиссии, налоги")
@RequestMapping("/api/v1/security-event-cash-flows")
public class SecurityEventCashFlowRestController extends AbstractRestController<Integer, SecurityEventCashFlow, SecurityEventCashFlowEntity> {
    private final FifoPositionsFactory positionsFactory;

    public SecurityEventCashFlowRestController(JpaRepository<SecurityEventCashFlowEntity, Integer> repository,
                                               EntityConverter<SecurityEventCashFlowEntity, SecurityEventCashFlow> converter,
                                               FifoPositionsFactory positionsFactory) {
        super(repository, converter);
        this.positionsFactory = positionsFactory;
    }

    @Override
    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить все", description = "Отображает все выплаты по всем счетам", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public Page<SecurityEventCashFlow> get(@Parameter(hidden = true)
                                           Pageable pageable) {
        return super.get(pageable);
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одну", description = "Отобразить выплату по идентификатору", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<SecurityEventCashFlow> get(@PathVariable("id")
                                                     @Parameter(description = "Внутренний идентификатор выплаты")
                                                     Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Сохранить информацию о выплате", responses = {
            @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
            @ApiResponse(responseCode = "409"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> post(@RequestBody @Valid SecurityEventCashFlow event) {
        if (event.getEventType() == REDEMPTION) positionsFactory.invalidateCache();
        return super.post(event);
    }

    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить", description = "Модифицировать информацию о выплате", responses = {
            @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор выплаты")
                                    Integer id,
                                    @RequestBody
                                    @Valid
                                    SecurityEventCashFlow event) {
        if (event.getEventType() == REDEMPTION) positionsFactory.invalidateCache();
        return super.put(id, event);
    }

    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удалить информацию о выплате", responses = {
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> delete(@PathVariable("id")
                                       @Parameter(description = "Внутренний идентификатор выплаты")
                                       Integer id) {
        positionsFactory.invalidateCache();
        return super.delete(id);
    }

    @Override
    public @Nullable Integer getId(SecurityEventCashFlow object) {
        return object.getId();
    }

    @Override
    protected SecurityEventCashFlow updateId(Integer id, SecurityEventCashFlow object) {
        return object.toBuilder().id(id).build();
    }

    @Override
    protected String getLocation() {
        return "/security-event-cash-flows";
    }
}
