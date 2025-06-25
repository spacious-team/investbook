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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Transaction;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
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
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
@Tag(name = "Движения ДС по сделкам", description = "Уплаченные и вырученные суммы в сделках")
@RequestMapping("/api/v1/transaction-cash-flows")
public class TransactionCashFlowRestController extends AbstractRestController<Integer, TransactionCashFlow, TransactionCashFlowEntity> {
    private final TransactionCashFlowRepository repository;
    private final TransactionCashFlowConverter converter;
    private final TransactionRestController transactionRestController;

    public TransactionCashFlowRestController(TransactionCashFlowRepository repository,
                                             TransactionCashFlowConverter converter,
                                             TransactionRestController transactionRestController) {
        super(repository, converter);
        this.repository = repository;
        this.converter = converter;
        this.transactionRestController = transactionRestController;
    }

    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить по фильтру", description = "Отобразить информацию о сделках",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    protected Page<TransactionCashFlow> get(
            @RequestParam(value = "portfolio", required = false)
            @Parameter(description = "Номер счета")
            @Nullable
            String portfolio,
            @RequestParam(value = "trade-id", required = false)
            @Parameter(description = "Номер сделки в системе учета брокера")
            @Nullable
            String tradeId,
            @RequestParam(value = "event-type", required = false)
            @Parameter(description = "Тип (стоимость/комиссия/НКД)", example = "Смотреть API \"Типы событий\"")
            @Nullable
            Integer eventType,
            @Parameter(hidden = true)
            @Nullable
            @QuerydslPredicate(root = TransactionCashFlowEntity.class)
            Predicate predicate,
            @Parameter(hidden = true)
            Pageable pageable
    ) {
        if (portfolio != null || tradeId != null || eventType != null) {
            return filterByEventType(
                    transactionRestController.get(portfolio, tradeId, predicate, Pageable.unpaged()),
                    eventType);
        }

        return super.get(predicate, pageable);
    }

    private Page<TransactionCashFlow> filterByEventType(Page<Transaction> transactions,
                                                        @Nullable Integer eventType) {

        List<TransactionCashFlow> transactionCashFlows = transactions
                .stream()
                .flatMap(transaction -> findTransactionCashFlow(transaction, eventType))
                .map(converter::fromEntity)
                .toList();
        return new PageImpl<>(transactionCashFlows);
    }

    private Stream<TransactionCashFlowEntity> findTransactionCashFlow(Transaction transaction,
                                                                      @Nullable Integer eventType) {
        @Nullable Integer id = transaction.getId();
        if (isNull(id)) {
            return Stream.empty();
        }
        return isNull(eventType) ?
                repository.findByTransactionId(id).stream() :
                repository.findByTransactionIdAndCashFlowType(id, CashFlowType.valueOf(eventType)).stream();
    }

    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одну", description = "Отобразить информацию о конкретной сделке",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<TransactionCashFlow> get(@PathVariable("id")
                                                   @Parameter(description = "Внутренний идентификатор сделки")
                                                   Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Добавить информацию об об объемах движения ДС по сделке",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
                    @ApiResponse(responseCode = "409"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> post(@RequestBody @Valid TransactionCashFlow object) {
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить", description = "Обновить информацию об об объемах движения ДС по сделке",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор сделки")
                                    Integer id,
                                    @RequestBody
                                    @Valid
                                    TransactionCashFlow object) {
        return super.put(id, object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = """
            Удалить информацию об об объемах движения ДС по сделке. Сама сделка не удаляется, ее нужно удалить своим API
            """,
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> delete(@PathVariable("id")
                                       @Parameter(description = "Внутренний идентификатор сделки")
                                       Integer id) {
        return super.delete(id);
    }

    @Override
    public @Nullable Integer getId(TransactionCashFlow object) {
        return object.getId();
    }

    @Override
    protected TransactionCashFlow updateId(Integer id, TransactionCashFlow object) {
        return object.toBuilder()
                .id(id)
                .build();
    }

    @Override
    protected String getLocation() {
        return "/transaction-cash-flows";
    }
}
