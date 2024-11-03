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
import lombok.SneakyThrows;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.LOCATION;

@RestController
@Tag(name = "Официальные обменные курсы", description = "История обменных курсов валют")
@RequestMapping("/api/v1/foreign-exchange-rates")
public class ForeignExchangeRateRestController extends AbstractRestController<ForeignExchangeRateEntityPk, ForeignExchangeRate, ForeignExchangeRateEntity> {
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    private final ForeignExchangeRateConverter foreignExchangeRateConverter;
    private final ForeignExchangeRateService foreignExchangeRateService;

    public ForeignExchangeRateRestController(ForeignExchangeRateRepository repository,
                                             ForeignExchangeRateConverter converter,
                                             ForeignExchangeRateService foreignExchangeRateService) {
        super(repository, converter);
        this.foreignExchangeRateRepository = repository;
        this.foreignExchangeRateConverter = converter;
        this.foreignExchangeRateService = foreignExchangeRateService;
    }

    @Override
    @GetMapping
    @PageableAsQueryParam
    @Operation(summary = "Отобразить все", description = "Отображает всю имеющуюся информацию по обменным курсам",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public Page<ForeignExchangeRate> get(@Parameter(hidden = true)
                                         Pageable pageable) {
        return super.get(pageable);
    }

    @GetMapping("/currency-pairs/{currency-pair}")
    @Operation(summary = "Отобразить по валюте",
            description = "Отображает всю имеющуюся информацию по обменному курсу заданной валютной пары",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    protected List<ForeignExchangeRate> get(@PathVariable("currency-pair")
                                            @Parameter(description = "Валютная пара")
                                            String currencyPair) {
        return foreignExchangeRateRepository.findByPkCurrencyPairOrderByPkDateDesc(currencyPair)
                .stream()
                .map(foreignExchangeRateConverter::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @GetMapping("/currency-pairs/{currency-pair}/dates/{date}")
    @Operation(summary = "Отобразить по валюте и дате", responses = {
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "500", content = @Content)})
    protected ResponseEntity<ForeignExchangeRate> get(@PathVariable("currency-pair")
                                                      @Parameter(description = "Валютная пара", example = "USDRUB")
                                                      String currencyPair,
                                                      @PathVariable("date")
                                                      @Parameter(description = "Дата", example = "2021-01-23")
                                                      @DateTimeFormat(pattern = "yyyy-MM-dd")
                                                      LocalDate date) {
        return super.get(getId(currencyPair, date));
    }

    @Override
    @PostMapping
    @Operation(summary = "Добавить", responses = {
            @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
            @ApiResponse(responseCode = "409"),
            @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> post(@RequestBody @Valid ForeignExchangeRate object) {
        foreignExchangeRateService.invalidateCache();
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @PutMapping("/currency-pairs/{currency-pair}/dates/{date}")
    @Operation(summary = "Обновить", description = "Обновляет информацию о курсе валюты за заданную дату",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = LOCATION)),
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> put(@PathVariable("currency-pair")
                                    @Parameter(description = "Валютная пара", example = "USDRUB")
                                    String currencyPair,
                                    @PathVariable("date")
                                    @Parameter(description = "Дата", example = "2021-01-23")
                                    @DateTimeFormat(pattern = "yyyy-MM-dd")
                                    LocalDate date,
                                    @RequestBody
                                    @Valid
                                    ForeignExchangeRate object) {
        foreignExchangeRateService.invalidateCache();
        return super.put(getId(currencyPair, date), object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @DeleteMapping("/currency-pairs/{currency-pair}/dates/{date}")
    @Operation(summary = "Удалить", description = "Удаляет информацию о курсе из БД",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "500", content = @Content)})
    public ResponseEntity<Void> delete(@PathVariable("currency-pair")
                                       @Parameter(description = "Валютная пара", example = "USDRUB")
                                       String currencyPair,
                                       @PathVariable("date")
                                       @Parameter(description = "Дата", example = "2021-01-23")
                                       @DateTimeFormat(pattern = "yyyy-MM-dd")
                                       LocalDate date) {
        foreignExchangeRateService.invalidateCache();
        return super.delete(getId(currencyPair, date));
    }

    @Override
    public ForeignExchangeRateEntityPk getId(ForeignExchangeRate object) {
        return getId(object.getCurrencyPair(), object.getDate());
    }

    private ForeignExchangeRateEntityPk getId(String currencyPair, LocalDate date) {
        ForeignExchangeRateEntityPk pk = new ForeignExchangeRateEntityPk();
        pk.setCurrencyPair(currencyPair);
        pk.setDate(date);
        return pk;
    }

    @Override
    protected ForeignExchangeRate updateId(ForeignExchangeRateEntityPk id, ForeignExchangeRate object) {
        return object.toBuilder()
                .currencyPair(id.getCurrencyPair())
                .date(id.getDate())
                .build();
    }

    @Override
    @SneakyThrows
    protected Optional<URI> getLocationURI(ForeignExchangeRate object) {
        URI uri = new URI(getLocation() + "/currency-pairs/" + object.getCurrencyPair() + "/dates/" + object.getDate());
        return Optional.of(uri);
    }

    @Override
    protected String getLocation() {
        return "/foreign-exchange-rates";
    }
}
