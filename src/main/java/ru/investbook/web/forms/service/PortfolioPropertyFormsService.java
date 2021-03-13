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

package ru.investbook.web.forms.service;

import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.springframework.stereotype.Service;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.PortfolioPropertyEntity;
import ru.investbook.repository.PortfolioPropertyRepository;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.forms.model.PortfolioPropertyModel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_RUB;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_USD;

@Service
@RequiredArgsConstructor
public class PortfolioPropertyFormsService implements FormsService<PortfolioPropertyModel> {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final PortfolioPropertyRepository portfolioPropertyRepository;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PortfolioConverter portfolioConverter;
    private final Collection<String> properties = Set.of(TOTAL_ASSETS_RUB.name(), TOTAL_ASSETS_USD.name());

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
        PortfolioPropertyEntity entity = portfolioPropertyRepository.save(
                portfolioPropertyConverter.toEntity(PortfolioProperty.builder()
                        .id(m.getId())
                        .portfolio(m.getPortfolio())
                        .timestamp(m.getDate().atStartOfDay(zoneId).toInstant())
                        .property(m.getTotalAssetsCurrency().toPortfolioProperty())
                        .value(m.getTotalAssets().toString())
                        .build()));
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
        PortfolioPropertyModel m = new PortfolioPropertyModel();
        m.setId(e.getId());
        m.setPortfolio(e.getPortfolio().getId());
        m.setDate(e.getTimestamp().atZone(zoneId).toLocalDate());
        m.setTotalAssets(BigDecimal.valueOf(Double.parseDouble(e.getValue())));
        m.setTotalAssetsCurrency(PortfolioPropertyModel.Currency.valueOf(PortfolioPropertyType.valueOf(e.getProperty())));
        return m;
    }
}
