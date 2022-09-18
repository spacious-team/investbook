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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.investbook.converter.PortfolioCashConverter;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.PortfolioCashEntity;
import ru.investbook.repository.PortfolioCashRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.specs.PortfolioCashSearchSpecification;
import ru.investbook.web.forms.model.PortfolioCashModel;
import ru.investbook.web.forms.model.filter.PortfolioCashFormFilterModel;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioCashFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final PortfolioCashRepository portfolioCashRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioCashConverter portfolioCashConverter;
    private final PortfolioConverter portfolioConverter;

    @Transactional(readOnly = true)
    public Optional<PortfolioCashModel> getById(Integer id) {
        return portfolioCashRepository.findById(id)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public Page<PortfolioCashModel> getPage(PortfolioCashFormFilterModel filter) {
        PortfolioCashSearchSpecification spec = PortfolioCashSearchSpecification.of(
                filter.getPortfolio(), filter.getDateFrom(), filter.getDateTo(), filter.getCurrency());

        Sort sort = Sort.by(asc("portfolio"), desc("timestamp"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return portfolioCashRepository.findAll(spec, page)
                .map(this::toModel);
    }

    @Transactional
    public void save(PortfolioCashModel m) {
        saveAndFlush(m.getPortfolio());
        PortfolioCash cash = PortfolioCash.builder()
                .id(m.getId())
                .portfolio(m.getPortfolio())
                .market(StringUtils.hasLength(m.getMarket()) ? m.getMarket() : "")
                .timestamp(m.getDate().atTime(m.getTime()).atZone(zoneId).toInstant())
                .value(m.getCash())
                .currency(m.getCurrency())
                .build();

        PortfolioCashEntity entity = portfolioCashConverter.toEntity(cash);
        entity = portfolioCashRepository.save(entity);
        m.setId(entity.getId()); // used in view
        portfolioCashRepository.flush();
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private PortfolioCashModel toModel(PortfolioCashEntity e) {
        PortfolioCashModel m = new PortfolioCashModel();
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio());
        m.setMarket(e.getMarket());
        ZonedDateTime zonedDateTime = e.getTimestamp().atZone(zoneId);
        m.setDate(zonedDateTime.toLocalDate());
        m.setTime(zonedDateTime.toLocalTime());
        m.setCash(e.getValue());
        m.setCurrency(e.getCurrency());
        return m;
    }

    @Transactional
    public void delete(Integer id) {
        portfolioCashRepository.deleteById(id);
        portfolioCashRepository.flush();
    }
}
