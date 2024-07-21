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
import org.spacious_team.broker.pojo.Transaction;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.repository.TransactionRepository;

import java.util.List;

@RestController
@Tag(name = "Сделки", description = "Операции купли/продажи биржевых инструментов")
@RequestMapping("/api/v1/transactions")
public class TransactionRestController extends AbstractRestController<Integer, Transaction, TransactionEntity> {
    private final TransactionRepository repository;
    private final TransactionConverter converter;
    private final FifoPositionsFactory positionsFactory;

    public TransactionRestController(TransactionRepository repository,
                                     TransactionConverter converter,
                                     FifoPositionsFactory positionsFactory) {
        super(repository, converter);
        this.repository = repository;
        this.converter = converter;
        this.positionsFactory = positionsFactory;
    }

    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить по фильтру", description = "Отображает сделки по счетам")
    public Page<Transaction> get(@RequestParam(value = "portfolio", required = false)
                                 @Parameter(description = "Идентификатор счета брокера")
                                 String portfolio,
                                 @RequestParam(value = "trade-id", required = false)
                                 @Parameter(description = "Номер сделки в системе учета брокера")
                                 String tradeId,
                                 @Parameter(hidden = true)
                                 Pageable pageable) {
        if (portfolio != null && tradeId != null) {
            return getByPortfolioAndTradeId(portfolio, tradeId);
        } else if (portfolio != null) {
            return getByPortfolio(portfolio, pageable);
        } else if (tradeId != null) {
            return getByTradeId(tradeId, pageable);
        } else {
            return super.get(pageable);
        }
    }

    private Page<Transaction> getByPortfolio(String portfolio, Pageable pageable) {
        return repository.findByPortfolio(portfolio, pageable)
                .map(converter::fromEntity);
    }

    private Page<Transaction> getByTradeId(String tradeId, Pageable pageable) {
        return repository.findByTradeId(tradeId, pageable)
                .map(converter::fromEntity);
    }

    private Page<Transaction> getByPortfolioAndTradeId(String portfolio, String tradeId) {
        List<Transaction> transactions = repository.findByPortfolioAndTradeId(portfolio, tradeId)
                .stream()
                .map(converter::fromEntity)
                .toList();
        return new PageImpl<>(transactions);
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @Override
    @GetMapping("{id}")
    @Operation(summary = "Отобразить одну", description = "Отображает одну сделку")
    public ResponseEntity<Transaction> get(@PathVariable("id")
                                           @Parameter(description = "Внутренний идентификатор сделки")
                                           Integer id) {
        return super.get(id);
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Сохраняет новую сделку в БД")
    public ResponseEntity<Void> post(@Valid @RequestBody Transaction object) {
        positionsFactory.invalidateCache();
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @Override
    @PutMapping("{id}")
    @Operation(summary = "Обновить параметры", description = "Обновляет параметры указанной сделки")
    public ResponseEntity<Void> put(@PathVariable("id")
                                    @Parameter(description = "Внутренний идентификатор сделки")
                                    Integer id,
                                    @Valid
                                    @RequestBody
                                    Transaction object) {
        positionsFactory.invalidateCache();
        return super.put(id, object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удаляет указанную сделку")
    public ResponseEntity<Void> delete(@PathVariable("id")
                       @Parameter(description = "Внутренний идентификатор сделки")
                       Integer id) {
        positionsFactory.invalidateCache();
        return super.delete(id);
    }

    @Override
    protected Integer getId(Transaction object) {
        return object.getId();
    }

    @Override
    protected Transaction updateId(Integer id, Transaction object) {
        return object.toBuilder()
                .id(id)
                .build();
    }

    @Override
    protected String getLocation() {
        return "/transactions";
    }
}
