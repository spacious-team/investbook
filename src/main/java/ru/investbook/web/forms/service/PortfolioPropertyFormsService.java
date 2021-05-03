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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Service;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.forms.model.PortfolioPropertyCashModel;
import ru.investbook.web.forms.model.PortfolioPropertyModel;
import ru.investbook.web.forms.model.PortfolioPropertyTotalAssetsModel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioPropertyFormsService implements FormsService<PortfolioPropertyModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PortfolioConverter portfolioConverter;
    private final Collection<String> properties = Set.of(TOTAL_ASSETS_RUB.name(), TOTAL_ASSETS_USD.name(), CASH.name());

    public Optional<PortfolioPropertyModel> getById(Integer id) {
        return portfolioPropertyRepository.findById(id)
                .map(this::toModel);
    }

    @Override
    public List<PortfolioPropertyModel> getAll() {
        return portfolioPropertyRepository.findByPropertyInOrderByTimestampDesc(properties)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public void save(PortfolioPropertyModel m) {
        saveAndFlush(m.getPortfolio());
        PortfolioProperty.PortfolioPropertyBuilder builder = PortfolioProperty.builder()
                .id(m.getId())
                .portfolio(m.getPortfolio())
                .timestamp(m.getDate().atStartOfDay(zoneId).toInstant());

        if (m instanceof PortfolioPropertyCashModel) {
            PortfolioPropertyCashModel c = (PortfolioPropertyCashModel) m;
            PortfolioCash.PortfolioCashBuilder b = PortfolioCash.builder().section("all");
            Collection<PortfolioCash> cash = new ArrayList<>();
            cash.add(b.value(c.getCashRub()).currency("RUB").build());
            cash.add(b.value(c.getCashUsd()).currency("USD").build());
            cash.add(b.value(c.getCashEur()).currency("EUR").build());
            cash.add(b.value(c.getCashGbp()).currency("GBP").build());
            cash.add(b.value(c.getCashChf()).currency("CHF").build());
            cash = cash.stream()
                    .filter(e -> e.getValue() != null && e.getValue().doubleValue() > 0.001)
                    .collect(Collectors.toList());
            builder.property(CASH)
                    .value(PortfolioCash.serialize(cash));
        } else if (m instanceof PortfolioPropertyTotalAssetsModel) {
            PortfolioPropertyTotalAssetsModel a = (PortfolioPropertyTotalAssetsModel) m;
            builder.property(a.getTotalAssetsCurrency().toPortfolioProperty())
                    .value(a.getTotalAssets().toString());
        } else {
            throw new IllegalArgumentException("Unexpected type " + m.getClass());
        }

        PortfolioPropertyEntity entity = portfolioPropertyConverter.toEntity(builder.build());
        entity = portfolioPropertyRepository.save(entity);
        m.setId(entity.getId());
        portfolioPropertyRepository.flush();
    }

    private void saveAndFlush(String portfolio) {
        if (!portfolioRepository.existsById(portfolio)) {
            portfolioRepository.saveAndFlush(
                    portfolioConverter.toEntity(Portfolio.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private PortfolioPropertyModel toModel(PortfolioPropertyEntity e) {
        PortfolioPropertyModel m;
        PortfolioPropertyType type = valueOf(e.getProperty().toUpperCase());
        m = switch (type) {
            case CASH -> new PortfolioPropertyCashModel();
            case TOTAL_ASSETS_RUB, TOTAL_ASSETS_USD -> new PortfolioPropertyTotalAssetsModel();
        };
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        return switch (type) {
            case CASH -> {
                PortfolioPropertyCashModel c = (PortfolioPropertyCashModel) m;
                try {
                    Map<String, BigDecimal> cashes = PortfolioCash.deserialize(e.getValue())
                            .stream()
                            .collect(groupingBy(p -> p.getCurrency().toUpperCase(),
                                    reducing(BigDecimal.ZERO, PortfolioCash::getValue, BigDecimal::add)));
                    c.setCashRub(cashes.getOrDefault("RUB", BigDecimal.ZERO));
                    c.setCashUsd(cashes.getOrDefault("USD", BigDecimal.ZERO));
                    c.setCashEur(cashes.getOrDefault("EUR", BigDecimal.ZERO));
                    c.setCashGbp(cashes.getOrDefault("GBP", BigDecimal.ZERO));
                    c.setCashChf(cashes.getOrDefault("CHF", BigDecimal.ZERO));
                } catch (Exception ex) {
                    log.warn("Ошибка при десериализации свойства: {}", e.getValue(), ex);
                }
                yield c;
            }
            case TOTAL_ASSETS_RUB, TOTAL_ASSETS_USD -> {
                PortfolioPropertyTotalAssetsModel a = (PortfolioPropertyTotalAssetsModel) m;
                a.setTotalAssets(BigDecimal.valueOf(Double.parseDouble(e.getValue())));
                a.setTotalAssetsCurrency(PortfolioPropertyTotalAssetsModel.Currency.valueOf(type));
                yield a;
            }
        };
    }
}
