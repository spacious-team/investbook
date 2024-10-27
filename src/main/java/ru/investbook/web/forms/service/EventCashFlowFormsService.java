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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.entity.EventCashFlowEntity_;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.specs.EventCashFlowEntitySearchSpecification;
import ru.investbook.web.forms.model.EventCashFlowModel;
import ru.investbook.web.forms.model.filter.EventCashFlowFormFilterModel;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Service
@RequiredArgsConstructor
public class EventCashFlowFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final EventCashFlowRepository eventCashFlowRepository;
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final PortfolioRepository portfolioRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final PortfolioConverter portfolioConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<EventCashFlowModel> getById(Integer id) {
        return eventCashFlowRepository.findById(id)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public Page<EventCashFlowModel> getPage(EventCashFlowFormFilterModel filter) {
        EventCashFlowEntitySearchSpecification spec = EventCashFlowEntitySearchSpecification.of(
                filter.getPortfolio(), filter.getDateFrom(), filter.getDateTo());

        Sort sort = Sort.by(Order.asc("portfolio.id"), Order.desc(EventCashFlowEntity_.TIMESTAMP));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return eventCashFlowRepository.findAll(spec, page)
                .map(this::toModel);
    }

    @Transactional
    public void save(EventCashFlowModel e) {
        savePortfolio(e.getPortfolio());
        if (e.isAttachedToSecurity()) {
            saveSecurityEventCashFlow(e);
        } else {
            saveEventCashFlow(e);
        }
    }

    private void saveSecurityEventCashFlow(EventCashFlowModel e) {
        int savedSecurityId = securityRepositoryHelper.saveSecurity(requireNonNull(e.getAttachedSecurity()));
        SecurityEventCashFlowEntity entity = securityEventCashFlowRepository.save(
                securityEventCashFlowConverter.toEntity(SecurityEventCashFlow.builder()
                        // no id(), it is always the new object
                        .portfolio(e.getPortfolio())
                        .timestamp(e.getDate().atTime(e.getTime()).atZone(zoneId).toInstant())
                        .security(savedSecurityId)
                        .count(requireNonNull(e.getAttachedSecurity().getCount()))
                        .eventType(e.getType())
                        .value(e.getValue())
                        .currency(e.getValueCurrency())
                        .build()));
        Optional.ofNullable(e.getId()).ifPresent(this::delete);
        e.setId(null);
        requireNonNull(e.getAttachedSecurity())
                .setSecurityEventCashFlowId(entity.getId()); // used in view
        securityEventCashFlowRepository.flush();
    }

    private void saveEventCashFlow(EventCashFlowModel e) {
        EventCashFlowEntity entity = eventCashFlowRepository.save(
                eventCashFlowConverter.toEntity(EventCashFlow.builder()
                        .id(e.getId())
                        .portfolio(e.getPortfolio())
                        .timestamp(e.getDate().atTime(e.getTime()).atZone(zoneId).toInstant())
                        .eventType(e.getType())
                        .value(e.getValue())
                        .currency(e.getValueCurrency())
                        .description(StringUtils.hasText(e.getDescription()) ? e.getDescription() : null)
                        .build()));
        e.setId(entity.getId()); // used in view
        eventCashFlowRepository.flush();
    }

    private void savePortfolio(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.save(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private EventCashFlowModel toModel(EventCashFlowEntity e) {
        CashFlowType type = CashFlowType.valueOf(e.getCashFlowType().getId());
        EventCashFlowModel m = new EventCashFlowModel();
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        ZonedDateTime zonedDateTime = e.getTimestamp().atZone(zoneId);
        m.setDate(zonedDateTime.toLocalDate());
        m.setTime(zonedDateTime.toLocalTime());
        m.setType(type);
        m.setValue(e.getValue());
        m.setValueCurrency(e.getCurrency());
        m.setDescription(e.getDescription());
        return m;
    }

    @Transactional
    public void delete(Integer id) {
        eventCashFlowRepository.deleteById(id);
        eventCashFlowRepository.flush();
    }
}
