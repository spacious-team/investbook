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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import ru.investbook.converter.TransactionConverter;
import ru.investbook.entity.TransactionEntity;
import ru.investbook.report.FifoPositionsFactory;
import ru.investbook.repository.TransactionRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    @Operation(summary = "Отобразить по фильтру", description = "Отображает сделки по счетам")
    public List<Transaction> get(@QuerydslPredicate(root = Transaction.class) @RequestParam(value = "portfolio", required = false)
                                        @Parameter(description = "Идентификатор счета брокера")
                                                String portfolio,
                                    @RequestParam(value = "trade-id", required = false)
                                        @Parameter(description = "Номер сделки в системе учета брокера")
                                                String tradeId,
                                    @RequestParam(value = "page", defaultValue = ApiUtil.DEFAULT_PAGE, required = false)
                                        @Parameter(description = "Номер страницы")
                                                int pageNo,
                                    @RequestParam(value = "size", defaultValue = ApiUtil.DEFAULT_PAGE_SIZE, required = false)
                                        @Parameter(description = "Количество записей на страницы")
                                                int pageSize,
                                    @RequestParam(value = "sortBy", defaultValue = ApiUtil.DEFAULT_TRANSACTION_SORT_BY, required = false)
                                        @Parameter(description = "атрибут сортировки")
                                                String sortBy,
                                    @RequestParam(value = "sortDir", defaultValue = ApiUtil.DEFAULT_SORT_DIRECTION, required = false)
                                        @Parameter(description = "направление сортировки")
                                                String sortDir) {
        if (portfolio != null && tradeId != null) {
            return getByPortfolioAndTradeId(portfolio, tradeId);
        } else if (portfolio != null) {
            return getByPortfolio(portfolio, getPage(pageNo, pageSize, sortBy, sortDir));
        } else if (tradeId != null) {
            return getByTradeId(tradeId, getPage(pageNo, pageSize, sortBy, sortDir));
        } else {
            return super.get(pageNo, pageSize, sortBy, sortDir);
        }
    }

    private List<Transaction> getByPortfolio(String portfolio, Pageable pageable) {
        return repository.findByPortfolio(portfolio, pageable)
                .getContent()
                .stream()
                .map(converter::fromEntity)
                .collect(Collectors.toList());
    }

    private List<Transaction> getByTradeId(String tradeId, Pageable pageable) {
        return repository.findByTradeId(tradeId, pageable)
                .getContent()
                .stream()
                .map(converter::fromEntity)
                .collect(Collectors.toList());
    }

    private List<Transaction> getByPortfolioAndTradeId(String portfolio, String tradeId) {
        return repository.findByPortfolioAndTradeId(portfolio, tradeId)
                .map(converter::fromEntity)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
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
                                    @Valid @RequestBody Transaction object) {
        positionsFactory.invalidateCache();
        return super.put(id, object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @Override
    @DeleteMapping("{id}")
    @Operation(summary = "Удалить", description = "Удаляет указанную сделку")
    public void delete(@PathVariable("id")
                       @Parameter(description = "Внутренний идентификатор сделки")
                               Integer id) {
        positionsFactory.invalidateCache();
        super.delete(id);
    }

    @Override
    protected Optional<TransactionEntity> getById(Integer id) {
        return repository.findById(id);
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

    private Pageable getPage(int pageNo, int pageSize, String sortBy, String sortDir){
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        return PageRequest.of(pageNo, pageSize, sort);
    }
}
