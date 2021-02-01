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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.TransactionCashFlow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.TransactionCashFlowConverter;
import ru.investbook.entity.TransactionCashFlowEntity;
import ru.investbook.entity.TransactionCashFlowEntityPK;
import ru.investbook.repository.TransactionCashFlowRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Движения ДС по сделкам", description = "Уплаченные и вырученные суммы в сделках")
@RequestMapping("/api/v1/transaction-cash-flows")
public class TransactionCashFlowRestController extends AbstractRestController<TransactionCashFlowEntityPK, TransactionCashFlow, TransactionCashFlowEntity> {
    private final TransactionCashFlowRepository transactionCashFlowRepository;

    public TransactionCashFlowRestController(TransactionCashFlowRepository repository,
                                             TransactionCashFlowConverter converter) {
        super(repository, converter);
        this.transactionCashFlowRepository = repository;
    }

    @Override
    @GetMapping
    @Operation(summary = "Отобразить все", description = "Отобразить информацию обо всех сделках")
    protected List<TransactionCashFlow> get() {
        return super.get();
    }

    @GetMapping("/portfolios/{portfolio}/ids/{transaction-id}")
    @Operation(summary = "Отобразить одну", description = "Отобразить информацию о конкретной сделке")
    protected List<TransactionCashFlow> get(@PathVariable("portfolio")
                                            @Parameter(description = "Номер счета")
                                                    String portfolio,
                                            @PathVariable("transaction-id")
                                            @Parameter(description = "Идентификатор сделки")
                                                    String transactionId) {
        return transactionCashFlowRepository.findByPkPortfolioAndPkTransactionId(portfolio, transactionId)
                .stream()
                .map(converter::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @GetMapping("/portfolios/{portfolio}/ids/{transaction-id}/events/{event-type}")
    @Operation(summary = "Отобразить детализацию одной", description = "Отобразить конкретную информацию о конкретной сделке")
    public ResponseEntity<TransactionCashFlow> get(@PathVariable("portfolio")
                                                   @Parameter(description = "Номер счета")
                                                           String portfolio,
                                                   @PathVariable("transaction-id")
                                                   @Parameter(description = "Идентификатор сделки")
                                                           String transactionId,
                                                   @PathVariable("event-type")
                                                   @Parameter(description = "Тип (стоимость/комиссия/НКД)", example = "Смотреть API \"Типы событий\"")
                                                           int eventType) {
        return super.get(getId(portfolio, transactionId, eventType));
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", description = "Добавить информацию об об объемах движения ДС по сделке")
    public ResponseEntity<Void> post(@RequestBody TransactionCashFlow object) {
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @PutMapping("/portfolios/{portfolio}/ids/{transaction-id}/events/{event-type}")
    @Operation(summary = "Обновить", description = "Обновить информацию об об объемах движения ДС по сделке")
    public ResponseEntity<Void> put(@PathVariable("portfolio")
                                    @Parameter(description = "Номер счета")
                                            String portfolio,
                                    @PathVariable("transaction-id")
                                    @Parameter(description = "Идентификатор сделки")
                                            String transactionId,
                                    @PathVariable("event-type")
                                    @Parameter(description = "Тип (стоимость/комиссия/НКД)", example = "Смотреть API \"Типы событий\"")
                                            int eventType,
                                    @RequestBody TransactionCashFlow object) {
        return super.put(getId(portfolio, transactionId, eventType), object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @DeleteMapping("/portfolios/{portfolio}/ids/{transaction-id}/events/{event-type}")
    @Operation(summary = "Удалить", description = """
            Удалить информацию об об объемах движения ДС по сделке. Сама сделка не удаляется, ее нужно удалить своим API
            """)
    public void delete(@PathVariable("portfolio")
                       @Parameter(description = "Номер счета")
                               String portfolio,
                       @PathVariable("transaction-id")
                       @Parameter(description = "Идентификатор сделки")
                               String transactionId,
                       @PathVariable("event-type")
                       @Parameter(description = "Тип (стоимость/комиссия/НКД)",
                               example = "Смотреть API \"Типы событий\"")
                               int eventType) {
        super.delete(getId(portfolio, transactionId, eventType));
    }

    @Override
    protected Optional<TransactionCashFlowEntity> getById(TransactionCashFlowEntityPK id) {
        return repository.findById(id);
    }

    @Override
    protected TransactionCashFlowEntityPK getId(TransactionCashFlow object) {
        return getId(object.getPortfolio(), object.getTransactionId(), object.getEventType().getId());
    }

    private TransactionCashFlowEntityPK getId(String portfolio, String transactionId, int eventType) {
        TransactionCashFlowEntityPK pk = new TransactionCashFlowEntityPK();
        pk.setTransactionId(transactionId);
        pk.setPortfolio(portfolio);
        pk.setType(eventType);
        return pk;
    }

    @Override
    protected TransactionCashFlow updateId(TransactionCashFlowEntityPK id, TransactionCashFlow object) {
        return object.toBuilder()
                .transactionId(id.getTransactionId())
                .portfolio(id.getPortfolio())
                .eventType(CashFlowType.valueOf(id.getType()))
                .build();
    }

    @Override
    protected URI getLocationURI(TransactionCashFlow object) throws URISyntaxException {
        return new URI(getLocation() + "/portfolios/" + object.getPortfolio()
                + "/ids/" + object.getTransactionId()
                + "/events/" + object.getEventType().getId());
    }

    @Override
    protected String getLocation() {
        return "/transaction-cash-flows";
    }
}
