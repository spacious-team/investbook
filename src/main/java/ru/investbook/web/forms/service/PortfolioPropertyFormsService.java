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
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.forms.model.PortfolioPropertyModel;
import ru.investbook.web.forms.model.PortfolioPropertyTotalAssetsModel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.PortfolioPropertyType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioPropertyFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PortfolioConverter portfolioConverter;
    private final Collection<String> properties = Set.of(TOTAL_ASSETS_RUB.name(), TOTAL_ASSETS_USD.name());

    @Transactional(readOnly = true)
    public Optional<PortfolioPropertyModel> getById(Integer id) {
        return portfolioPropertyRepository.findById(id)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public List<PortfolioPropertyModel> getAll() {
        return portfolioPropertyRepository
                .findByPortfolioInAndPropertyInOrderByTimestampDesc(
                        portfolioRepository.findByEnabledIsTrue(),
                        properties)
                .stream()
                .map(this::toModel)
                .collect(Collectors.toList());
    }

    @Transactional
    public void save(PortfolioPropertyModel m) {
        saveAndFlush(m.getPortfolio());
        PortfolioProperty.PortfolioPropertyBuilder builder = PortfolioProperty.builder()
                .id(m.getId())
                .portfolio(m.getPortfolio())
                .timestamp(m.getDate().atTime(m.getTime()).atZone(zoneId).toInstant());

        if (m instanceof PortfolioPropertyTotalAssetsModel a) {
            builder.property(a.getTotalAssetsCurrency().toPortfolioProperty())
                    .value(a.getTotalAssets().toString());
        } else {
            throw new IllegalArgumentException("Unexpected type " + m.getClass());
        }

        PortfolioPropertyEntity entity = portfolioPropertyConverter.toEntity(builder.build());
        entity = portfolioPropertyRepository.save(entity);
        m.setId(entity.getId()); // used in view
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
            case TOTAL_ASSETS_RUB, TOTAL_ASSETS_USD -> new PortfolioPropertyTotalAssetsModel();
        };
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        ZonedDateTime zonedDateTime = e.getTimestamp().atZone(zoneId);
        m.setDate(zonedDateTime.toLocalDate());
        m.setTime(zonedDateTime.toLocalTime());
        return switch (type) {
            case TOTAL_ASSETS_RUB, TOTAL_ASSETS_USD -> {
                PortfolioPropertyTotalAssetsModel a = (PortfolioPropertyTotalAssetsModel) m;
                a.setTotalAssets(getBigDecimalValue(e));
                a.setTotalAssetsCurrency(PortfolioPropertyTotalAssetsModel.Currency.valueOf(type));
                yield a;
            }
        };
    }

    private static BigDecimal getBigDecimalValue(PortfolioPropertyEntity entity) {
        try {
            return BigDecimal.valueOf(
                    Double.parseDouble(
                            entity.getValue()));
        } catch (Exception e) {
            log.error("Значение должно содержать число, сохранено {}", entity);
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    public void delete(Integer id) {
        portfolioPropertyRepository.deleteById(id);
        portfolioPropertyRepository.flush();
    }
}
