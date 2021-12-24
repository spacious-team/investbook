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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import java.util.Optional;
import java.util.function.Supplier;

import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.repository.RepositoryHelper.isUniqIndexViolationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRegistrarImpl implements SecurityRegistrar {
    private final SecurityRepository repository;
    private final SecurityConverter converter;
    private final MoexDerivativeCodeService derivativeCodeService;

    @Cacheable(cacheNames = "declareStock", key = "#isin")
    @Override
    public int declareStock(String isin, Supplier<SecurityBuilder> supplier) {
        return declareIsinSecurity(isin, STOCK, supplier);
    }

    @Cacheable(cacheNames = "declareBond", key = "#isin")
    @Override
    public int declareBond(String isin, Supplier<SecurityBuilder> supplier) {
        return declareIsinSecurity(isin, BOND, supplier);
    }

    @Cacheable(cacheNames = "declareStockOrBond", key = "#isin")
    @Override
    public int declareStockOrBond(String isin, Supplier<SecurityBuilder> supplier) {
        return declareIsinSecurity(isin, STOCK_OR_BOND, supplier);
    }

    private int declareIsinSecurity(String isin, SecurityType defaultType, Supplier<SecurityBuilder> supplier) {
        return repository.findByIsin(isin)
                .or(() -> Optional.of(supplier.get())
                        .map(builder -> buildSecurity(builder, defaultType))
                        .map(security -> saveAndFlush(security, () -> repository.findByIsin(isin))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить ЦБ с ISIN = " + isin));
    }

    @Cacheable(cacheNames = "declareDerivative", key = "#code")
    @Override
    public int declareDerivative(String code) {
        String shortNameIfCan = derivativeCodeService.convertDerivativeCode(code);
        return repository.findByTicker(shortNameIfCan)
                .or(() -> Optional.of(Security.builder().ticker(shortNameIfCan).type(DERIVATIVE).build())
                        .map(security -> saveAndFlush(security, () -> repository.findByTicker(shortNameIfCan))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить срочный контракт " + code));
    }

    @Cacheable(cacheNames = "declareCurrencyPair", key = "#contract")
    @Override
    public int declareCurrencyPair(String contract) {
        return repository.findByTicker(contract)
                .or(() -> Optional.of(Security.builder().ticker(contract).type(CURRENCY_PAIR).build())
                        .map(security -> saveAndFlush(security, () -> repository.findByTicker(contract))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить контракт валютного рынка " + contract));
    }

    @Cacheable(cacheNames = "declareAsset", key = "#assetName")
    @Override
    public int declareAsset(String assetName, Supplier<SecurityBuilder> supplier) {
        return repository.findByName(assetName)
                .or(() -> Optional.of(supplier.get())
                        .map(builder -> buildSecurity(builder, ASSET))
                        .map(security -> saveAndFlush(security, () -> repository.findByName(assetName))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить произвольный актив " + assetName));
    }

    private Security buildSecurity(SecurityBuilder builder, SecurityType defaultType) {
        Security security = builder.build();
        if (security.getType() == null) {
            security = builder.type(defaultType).build();
        }
        return security;
    }

    private SecurityEntity saveAndFlush(Security security, Supplier<Optional<SecurityEntity>> supplier) {
        try {
            return repository.saveAndFlush(converter.toEntity(security));
        } catch (Exception e) {
            if (isUniqIndexViolationException(e)) {
                log.trace("Дублирование вызвано исключением", e);
                return supplier.get()
                        .orElseThrow(() -> new RuntimeException("Не смог сохранить ценную бумагу в БД: " + security, e));
            }
            throw new RuntimeException("Не смог сохранить ценную бумагу в БД: " + security, e);
        }
    }
}