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
import org.spacious_team.broker.pojo.Account;
import org.spacious_team.broker.pojo.AccountProperty;
import org.spacious_team.broker.pojo.AccountPropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.converter.PortfolioConverter;
import ru.investbook.converter.PortfolioPropertyConverter;
import ru.investbook.entity.AccountPropertyEntity;
import ru.investbook.entity.AccountPropertyEntity_;
import ru.investbook.repository.AccountPropertyRepository;
import ru.investbook.repository.AccountRepository;
import ru.investbook.repository.specs.AccountPropertySearchSpecification;
import ru.investbook.web.forms.model.PortfolioPropertyModel;
import ru.investbook.web.forms.model.PortfolioPropertyTotalAssetsModel;
import ru.investbook.web.forms.model.filter.PortfolioPropertyFormFilterModel;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.spacious_team.broker.pojo.AccountPropertyType.valueOf;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortfolioPropertyFormsService {
    private static final ZoneId zoneId = ZoneId.systemDefault();
    private final AccountPropertyRepository accountPropertyRepository;
    private final AccountRepository accountRepository;
    private final PortfolioPropertyConverter portfolioPropertyConverter;
    private final PortfolioConverter portfolioConverter;

    @Transactional(readOnly = true)
    public Optional<PortfolioPropertyModel> getById(Integer id) {
        return accountPropertyRepository.findById(id)
                .map(this::toModel);
    }

    @Transactional(readOnly = true)
    public Page<PortfolioPropertyModel> getPage(PortfolioPropertyFormFilterModel filter) {
        AccountPropertySearchSpecification spec = AccountPropertySearchSpecification.of(
                filter.getPortfolio(), filter.getDate(), filter.getProperty());

        Sort sort = Sort.by(asc(AccountPropertyEntity_.ACCOUNT), desc(AccountPropertyEntity_.TIMESTAMP));
        PageRequest page = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        return accountPropertyRepository.findAll(spec, page)
                .map(this::toModel);
    }

    @Transactional
    public void save(PortfolioPropertyModel m) {
        savePortfolio(m.getPortfolio());
        AccountProperty.AccountPropertyBuilder builder = AccountProperty.builder()
                .id(m.getId())
                .account(m.getPortfolio())
                .timestamp(m.getDate().atTime(m.getTime()).atZone(zoneId).toInstant());

        if (m instanceof PortfolioPropertyTotalAssetsModel a) {
            builder.property(a.getTotalAssetsCurrency().toAccountProperty())
                    .value(a.getTotalAssets().toString());
        } else {
            throw new IllegalArgumentException("Unexpected type " + m.getClass());
        }

        AccountPropertyEntity entity = portfolioPropertyConverter.toEntity(builder.build());
        entity = accountPropertyRepository.save(entity);
        m.setId(entity.getId()); // used in view
        accountPropertyRepository.flush();
    }

    private void savePortfolio(String portfolio) {
        if (!accountRepository.existsById(portfolio)) {
            accountRepository.save(
                    portfolioConverter.toEntity(Account.builder()
                            .id(portfolio)
                            .build()));
        }
    }

    private PortfolioPropertyModel toModel(AccountPropertyEntity e) {
        PortfolioPropertyModel m;
        AccountPropertyType type = valueOf(e.getProperty().toUpperCase());
        m = switch (type) {
            case TOTAL_ASSETS_RUB, TOTAL_ASSETS_USD -> new PortfolioPropertyTotalAssetsModel();
        };
        m.setId(e.getId());
        m.setPortfolio(e.getAccount().getId());
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

    private static BigDecimal getBigDecimalValue(AccountPropertyEntity entity) {
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
        accountPropertyRepository.deleteById(id);
        accountPropertyRepository.flush();
    }
}
