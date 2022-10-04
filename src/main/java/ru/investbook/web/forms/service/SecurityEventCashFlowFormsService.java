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
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityEventCashFlow.SecurityEventCashFlowBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.specs.SecurityEventCashFlowEntitySearchSpecification;
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;
import ru.investbook.web.forms.model.filter.SecurityEventCashFlowFormFilterModel;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

@Service
@RequiredArgsConstructor
public class SecurityEventCashFlowFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final PortfolioRepository portfolioRepository;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final PortfolioConverter portfolioConverter;
    private final SecurityRepositoryHelper securityRepositoryHelper;

    @Transactional(readOnly = true)
    public Optional<SecurityEventCashFlowModel> getById(Integer id) {
        return securityEventCashFlowRepository.findById(id)
                .map(this::toSecurityEventModel);
    }

    @Transactional(readOnly = true)
    public Page<SecurityEventCashFlowModel> getPage(SecurityEventCashFlowFormFilterModel filter) {
        SecurityEventCashFlowEntitySearchSpecification spec =
                SecurityEventCashFlowEntitySearchSpecification.of(
                        filter.getPortfolio(), filter.getSecurity(), filter.getDateFrom(), filter.getDateTo());

        Sort sort = Sort.by(asc("portfolio.id"), desc("timestamp"), asc("security.id"));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return securityEventCashFlowRepository.findAll(spec, page)
                .map(this::toSecurityEventModel);
    }

    @Transactional
    public void save(SecurityEventCashFlowModel e) {
        saveAndFlush(e.getPortfolio());
        int savedSecurityId = securityRepositoryHelper.saveAndFlushSecurity(e);
        SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .portfolio(e.getPortfolio())
                .timestamp(e.getDate().atTime(e.getTime()).atZone(zoneId).toInstant())
                .security(savedSecurityId)
                .count(e.getCount());
        SecurityEventCashFlowEntity entity = securityEventCashFlowRepository.save(
                securityEventCashFlowConverter.toEntity(builder
                        .id(e.getId())
                        .eventType(e.getType())
                        .value(e.getValue())
                        .currency(e.getValueCurrency())
                        .build()));
        e.setId(entity.getId()); // used in view
        if (e.getTax() != null && e.getTax().floatValue() > 0.001) {
            entity = securityEventCashFlowRepository.save(securityEventCashFlowConverter.toEntity(
                    builder
                            .id(e.getTaxId())
                            .eventType(CashFlowType.TAX)
                            .value(e.getTax().negate())
                            .currency(e.getTaxCurrency())
                            .build()));
            e.setTaxId(entity.getId());
        } else if (e.getTaxId() != null) { // taxId exists in db, but no tax value in edited version
            securityEventCashFlowRepository.deleteById(e.getTaxId());
        }
        securityEventCashFlowRepository.flush();
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private SecurityEventCashFlowModel toSecurityEventModel(SecurityEventCashFlowEntity e) {
        SecurityEventCashFlowModel m = new SecurityEventCashFlowModel();
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        ZonedDateTime zonedDateTime = e.getTimestamp().atZone(zoneId);
        m.setDate(zonedDateTime.toLocalDate());
        m.setTime(zonedDateTime.toLocalTime());
        m.setCount(e.getCount());
        CashFlowType type = CashFlowType.valueOf(e.getCashFlowType().getId());
        m.setType(type);
        SecurityEntity securityEntity = e.getSecurity();
        m.setSecurity(
                securityEntity.getIsin(),
                ofNullable(securityEntity.getName()).orElse(securityEntity.getTicker()),
                m.getSecurityType());
        m.setValue(type == DERIVATIVE_PROFIT ? e.getValue() : e.getValue().abs());
        m.setValueCurrency(e.getCurrency());

        if (m.getType() != CashFlowType.TAX) {
            securityEventCashFlowRepository.findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampAndCount(
                            m.getPortfolio(),
                            securityEntity.getId(),
                            CashFlowType.TAX.getId(),
                            e.getTimestamp(),
                            m.getCount())
                    .ifPresent(tax -> {
                        m.setTaxId(tax.getId());
                        m.setTax(tax.getValue().abs());
                        m.setTaxCurrency(tax.getCurrency());
                    });
        }
        return m;
    }

    @Transactional
    public void delete(Integer id) {
        getById(id).map(SecurityEventCashFlowModel::getTaxId)
                .ifPresent(securityEventCashFlowRepository::deleteById);
        securityEventCashFlowRepository.deleteById(id);
        securityEventCashFlowRepository.flush();
    }
}
