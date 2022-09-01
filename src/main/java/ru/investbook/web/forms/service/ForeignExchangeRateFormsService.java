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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.ForeignExchangeRateConverter;
import ru.investbook.entity.ForeignExchangeRateEntity;
import ru.investbook.entity.ForeignExchangeRateEntityPk;
import ru.investbook.repository.ForeignExchangeRateRepository;
import ru.investbook.repository.specs.ForeignExchangeRateSearchSpecification;
import ru.investbook.web.forms.model.ForeignExchangeRateModel;
import ru.investbook.web.forms.model.filter.ForeignExchangeRateFormFilterModel;

import java.time.LocalDate;
import java.util.Optional;

import static java.lang.Math.min;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

@Service
@RequiredArgsConstructor
public class ForeignExchangeRateFormsService {
    private final ForeignExchangeRateRepository foreignExchangeRateRepository;
    private final ForeignExchangeRateConverter foreignExchangeRateConverter;

    @Transactional(readOnly = true)
    public Optional<ForeignExchangeRateModel> getById(LocalDate date, String baseCurrency, String quoteCurrency) {
        ForeignExchangeRateEntityPk pk = new ForeignExchangeRateEntityPk();
        pk.setDate(date);
        pk.setCurrencyPair(baseCurrency + quoteCurrency);
        return foreignExchangeRateRepository.findById(pk)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public Page<ForeignExchangeRateModel> getPage(ForeignExchangeRateFormFilterModel filter) {
        ForeignExchangeRateSearchSpecification spec =
                ForeignExchangeRateSearchSpecification.of(filter.getCurrency(), filter.getDate());

        Sort sort = Sort.by(desc("pk.date"), asc("pk.currencyPair"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return foreignExchangeRateRepository.findAll(spec, page)
                .map(this::toModel);
    }

    @Transactional
    public void save(ForeignExchangeRateModel m) {
        foreignExchangeRateRepository.saveAndFlush(
                foreignExchangeRateConverter.toEntity(ForeignExchangeRate.builder()
                        .date(m.getDate())
                        .currencyPair((m.getBaseCurrency() + m.getQuoteCurrency()).toUpperCase())
                        .rate(m.getRate().abs())
                        .build()));
    }

    private ForeignExchangeRateModel toModel(ForeignExchangeRateEntity e) {
        String currencyPair = e.getPk().getCurrencyPair();
        ForeignExchangeRateModel m = new ForeignExchangeRateModel();
        m.setDate(e.getPk().getDate());
        m.setBaseCurrency(currencyPair.substring(0, min(3, currencyPair.length())));
        m.setQuoteCurrency(currencyPair.substring(min(3, currencyPair.length()), min(6, currencyPair.length())));
        m.setRate(e.getRate());
        return m;
    }

    @Transactional
    public void delete(LocalDate date, String baseCurrency, String quoteCurrency) {
        ForeignExchangeRateEntityPk pk = new ForeignExchangeRateEntityPk();
        pk.setDate(date);
        pk.setCurrencyPair(baseCurrency + quoteCurrency);
        foreignExchangeRateRepository.deleteById(pk);
        foreignExchangeRateRepository.flush();
    }
}
