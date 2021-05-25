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
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityEventCashFlow.SecurityEventCashFlowBuilder;
import org.springframework.stereotype.Service;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.converter.SecurityEventCashFlowConverter;
import ru.investbook.entity.SecurityEventCashFlowEntity;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.repository.SecurityEventCashFlowRepository;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexDerivativeCodeService;
import ru.investbook.web.forms.model.SecurityEventCashFlowModel;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
@RequiredArgsConstructor
public class SecurityEventCashFlowFormsService implements FormsService<SecurityEventCashFlowModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityRepository securityRepository;
    private final PortfolioRepository portfolioRepository;
    private final SecurityEventCashFlowConverter securityEventCashFlowConverter;
    private final SecurityConverter securityConverter;
    private final PortfolioConverter portfolioConverter;
    private final MoexDerivativeCodeService moexDerivativeCodeService;

    public Optional<SecurityEventCashFlowModel> getById(Integer id) {
        return securityEventCashFlowRepository.findById(id)
                .map(this::toSecurityEventModel);
    }

    @Override
    public List<SecurityEventCashFlowModel> getAll() {
        return securityEventCashFlowRepository.findByOrderByPortfolioIdAscTimestampDescSecurityIdAsc()
                .stream()
                .filter(e -> e.getCashFlowType().getId() != CashFlowType.TAX.getId())
                .map(this::toSecurityEventModel)
                .collect(Collectors.toList());
    }

    @Override
    public void save(SecurityEventCashFlowModel e) {
        convertDerivativeSecurityId(e);
        saveAndFlush(e.getPortfolio(), e.getSecurityId(), e.getSecurityName());
        SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .portfolio(e.getPortfolio())
                .timestamp(e.getDate().atStartOfDay(zoneId).toInstant())
                .security(e.getSecurityId())
                .count(e.getCount());
        SecurityEventCashFlowEntity entity = securityEventCashFlowRepository.save(
                securityEventCashFlowConverter.toEntity(builder
                        .id(e.getId())
                        .eventType(e.getType())
                        .value(e.getValue())
                        .currency(e.getValueCurrency())
                        .build()));
        e.setId(entity.getId());
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

    private void convertDerivativeSecurityId(SecurityEventCashFlowModel model) {
        String security = model.getSecurity();
        if (moexDerivativeCodeService.isFuturesCode(security)) {
            security = moexDerivativeCodeService.convertDerivativeSecurityId(security);
            model.setSecurity(security);
        }
    }

    private void saveAndFlush(String portfolio, String securityId, String securityName) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
        if (!securityRepository.existsById(securityId)) {
            securityRepository.saveAndFlush(
                    securityConverter.toEntity(Security.builder()
                            .id(securityId)
                            .name(securityName)
                            .build()));
        }
    }

    private SecurityEventCashFlowModel toSecurityEventModel(SecurityEventCashFlowEntity e) {
        SecurityEventCashFlowModel m = new SecurityEventCashFlowModel();
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        m.setSecurity(e.getSecurity().getId(), ofNullable(e.getSecurity().getName()).orElse(e.getSecurity().getTicker()));
        m.setCount(e.getCount());
        m.setType(CashFlowType.valueOf(e.getCashFlowType().getId()));
        m.setValue(e.getValue().abs());
        m.setValueCurrency(e.getCurrency());

        if (m.getType() != CashFlowType.TAX) {
            securityEventCashFlowRepository.findByPortfolioIdAndSecurityIdAndCashFlowTypeIdAndTimestampAndCount(
                    m.getPortfolio(),
                    m.getSecurityId(),
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
}
