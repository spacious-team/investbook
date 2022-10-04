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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;
import org.spacious_team.broker.pojo.SecurityType;
import org.springframework.stereotype.Service;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.entity.SecurityEntity;
import ru.investbook.repository.SecurityRepository;
import ru.investbook.service.moex.MoexDerivativeCodeService;

import javax.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.spacious_team.broker.pojo.SecurityType.*;
import static ru.investbook.repository.RepositoryHelper.isUniqIndexViolationException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityRegistrarImpl implements SecurityRegistrar {
    private final SecurityRepository repository;
    private final SecurityConverter converter;
    private final MoexDerivativeCodeService derivativeCodeService;
    private final ValidatorService validator;

    @Override
    public int declareStockByIsin(String isin, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByIsin(isin, STOCK, supplier);
    }

    @Override
    public int declareBondByIsin(String isin, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByIsin(isin, BOND, supplier);
    }

    @Override
    public int declareStockOrBondByIsin(String isin, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByIsin(isin, STOCK_OR_BOND, supplier);
    }

    @Override
    public int declareStockByName(String name, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByName(name, STOCK, supplier);
    }

    @Override
    public int declareBondByName(String name, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByName(name, BOND, supplier);
    }

    @Override
    public int declareStockOrBondByName(String name, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByName(name, STOCK_OR_BOND, supplier);
    }


    @Override
    public int declareStockByTicker(String ticker, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByTicker(ticker, STOCK, supplier);
    }

    @Override
    public int declareBondByTicker(String ticker, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByTicker(ticker, BOND, supplier);
    }

    @Override
    public int declareStockOrBondByTicker(String ticker, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByTicker(ticker, STOCK_OR_BOND, supplier);
    }

    @Override
    public int declareDerivative(String code) {
        String shortNameIfCan = derivativeCodeService.convertDerivativeCode(code);
        return declareContractByTicker(shortNameIfCan, DERIVATIVE);
    }

    @Override
    public int declareCurrencyPair(String contract) {
        return declareContractByTicker(contract, CURRENCY_PAIR);
    }

    @Override
    public int declareAsset(String assetName, Supplier<SecurityBuilder> supplier) {
        return declareSecurityByName(assetName, ASSET, supplier);
    }

    @Override
    public int declareSecurity(Security security) {
        return switch (security.getType()) {
            case STOCK, BOND, STOCK_OR_BOND -> ofNullable(security.getIsin())
                    .map(isin -> declareSecurityByIsin(isin, security.getType(), security::toBuilder))
                    .or(() -> ofNullable(security.getTicker())
                            .map(ticker -> declareSecurityByTicker(ticker, security.getType(), security::toBuilder)))
                    .or(() -> ofNullable(security.getName())
                            .map(name -> declareSecurityByName(name, security.getType(), security::toBuilder)))
                    .orElseThrow();
            case DERIVATIVE, CURRENCY_PAIR -> declareContractByTicker(security.getTicker(), security.getType());
            case ASSET -> declareAsset(security.getName(), security::toBuilder);
        };
    }

    private int declareSecurityByIsin(String isin, SecurityType defaultType, Supplier<SecurityBuilder> supplier) {
        return repository.findByIsin(isin)
                .or(() -> Optional.of(supplier.get())
                        .map(builder -> buildSecurity(builder, defaultType))
                        .map(security -> saveAndFlush(security, () -> repository.findByIsin(isin))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить ЦБ с ISIN = " + isin));
    }

    private Integer declareSecurityByName(String name, SecurityType defaultType, Supplier<SecurityBuilder> supplier) {
        return repository.findByName(name)
                .or(() -> Optional.of(supplier.get())
                        .map(builder -> buildSecurity(builder, defaultType))
                        .map(security -> saveAndFlush(security, () -> repository.findByName(name))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить актив с наименованием = " + name));
    }

    private Integer declareSecurityByTicker(String ticker, SecurityType defaultType, Supplier<SecurityBuilder> supplier) {
        return repository.findByTicker(ticker)
                .or(() -> Optional.of(supplier.get())
                        .map(builder -> buildSecurity(builder, defaultType))
                        .map(security -> saveAndFlush(security, () -> repository.findByTicker(ticker))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить актив с тикером = " + ticker));
    }

    private Integer declareContractByTicker(String contract, SecurityType contractType) {
        return repository.findByTicker(contract)
                .or(() -> Optional.of(Security.builder().ticker(contract).type(contractType).build())
                        .map(security -> saveAndFlush(security, () -> repository.findByTicker(contract))))
                .map(SecurityEntity::getId)
                .orElseThrow(() -> new RuntimeException("Не смог сохранить контракт = " + contract));
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
            validator.validate(security);
            return repository.saveAndFlush(converter.toEntity(security));
        } catch (ConstraintViolationException e) {
            throw new RuntimeException("Не смог сохранить ценную бумагу в БД: " + security + ", " + e.getMessage());
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
