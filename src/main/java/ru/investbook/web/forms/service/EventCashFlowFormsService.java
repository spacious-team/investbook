/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.investbook.converter.EventCashFlowConverter;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.entity.EventCashFlowEntity;
import ru.investbook.repository.EventCashFlowRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.forms.model.EventCashFlowModel;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventCashFlowFormsService implements FormsService<EventCashFlowModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final EventCashFlowRepository eventCashFlowRepository;
    private final PortfolioRepository portfolioRepository;
    private final EventCashFlowConverter eventCashFlowConverter;
    private final PortfolioConverter portfolioConverter;

    public Optional<EventCashFlowModel> getById(Integer id) {
        return eventCashFlowRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    public List<EventCashFlowModel> getAll() {
        return eventCashFlowRepository
                .findByPortfolioInOrderByPortfolioIdAscTimestampDesc(portfolioRepository.findByEnabledIsTrue())
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public void save(EventCashFlowModel e) {
        saveAndFlush(e.getPortfolio());
        EventCashFlowEntity entity = eventCashFlowRepository.save(
                eventCashFlowConverter.toEntity(EventCashFlow.builder()
                        .id(e.getId())
                        .portfolio(e.getPortfolio())
                        .timestamp(e.getDate().atStartOfDay(zoneId).toInstant())
                        .eventType(e.getType())
                        .value(e.getValue())
                        .currency(e.getValueCurrency())
                        .description(StringUtils.hasText(e.getDescription()) ? e.getDescription() : null)
                        .build()));
        e.setId(entity.getId());
        eventCashFlowRepository.flush();
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
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
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        m.setType(type);
        m.setValue(e.getValue());
        m.setValueCurrency(e.getCurrency());
        m.setDescription(e.getDescription());
        return m;
    }

    public void delete(Integer id) {
        eventCashFlowRepository.deleteById(id);
        eventCashFlowRepository.flush();
    }
}
