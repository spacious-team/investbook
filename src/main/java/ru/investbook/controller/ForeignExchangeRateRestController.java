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

import org.spacious_team.broker.pojo.ForeignExchangeRate;
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
import ru.investbook.repository.ForeignExchangeRateRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/foreign-exchange-rate")
public class ForeignExchangeRateRestController extends AbstractRestController<ForeignExchangeRateEntityPk, ForeignExchangeRate, ForeignExchangeRateEntity> {
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;

    public ForeignExchangeRateRestController(ForeignExchangeRateRepository repository,
                                             ForeignExchangeRateConverter converter) {
        super(repository, converter);
        this.foreignExchangeRateRepository = repository;
    }

    @GetMapping
    @Override
    protected List<ForeignExchangeRateEntity> get() {
        return super.get();
    }

    @GetMapping("/currency-pair/{currency-pair}")
    protected List<ForeignExchangeRateEntity> get(@PathVariable("currency-pair") String currencyPair) {
        return foreignExchangeRateRepository.findByPkCurrencyPairOrderByPkDateDesc(currencyPair);
    }

    /**
     * see {@link AbstractRestController#get(Object)}
     */
    @GetMapping("/currency-pair/{currency-pair}/date/{date}")
    protected ResponseEntity<ForeignExchangeRateEntity> get(@PathVariable("currency-pair") String currencyPair,
                                                            @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return super.get(getId(currencyPair, date));
    }

    @PostMapping
    @Override
    public ResponseEntity<ForeignExchangeRateEntity> post(@RequestBody ForeignExchangeRate object) {
        return super.post(object);
    }

    /**
     * see {@link AbstractRestController#put(Object, Object)}
     */
    @PutMapping("/currency-pair/{currency-pair}/date/{date}")
    public ResponseEntity<ForeignExchangeRateEntity> put(@PathVariable("currency-pair") String currencyPair,
                                                         @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                         @RequestBody ForeignExchangeRate object) {
        return super.put(getId(currencyPair, date), object);
    }

    /**
     * see {@link AbstractRestController#delete(Object)}
     */
    @DeleteMapping("/currency-pair/{currency-pair}/date/{date}")
    public void delete(@PathVariable("currency-pair") String currencyPair,
                       @PathVariable("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        super.delete(getId(currencyPair, date));
    }

    @Override
    protected Optional<ForeignExchangeRateEntity> getById(ForeignExchangeRateEntityPk id) {
        return repository.findById(id);
    }

    @Override
    protected ForeignExchangeRateEntityPk getId(ForeignExchangeRate object) {
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
    protected URI getLocationURI(ForeignExchangeRate object) throws URISyntaxException {
        return new URI(getLocation() + "/currency-pair/" + object.getCurrencyPair()
                + "/date/"+ object.getDate());
    }

    @Override
    protected String getLocation() {
        return "/foreign-exchange-rate";
    }
}
