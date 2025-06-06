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

package ru.investbook.parser.sber;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import ru.investbook.parser.SecurityRegistrar;

import java.util.Optional;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.entity.SecurityEntity.isinPattern;


@Slf4j
public class SecurityHelper {

    public static Security getSecurity(String code, @Nullable String securityName, String section, @Nullable String type,
                                       SecurityRegistrar securityRegistrar) {
        @Nullable String securityId = getSecurityId(code, section);
        SecurityType securityType = getSecurityType(section, type);
        Security.SecurityBuilder security = Security.builder().type(securityType);
        int id = switch (securityType) {
            case STOCK, BOND, STOCK_OR_BOND -> {
                security
                        .isin(securityId)
                        .name(securityName);
                if (securityId == null) {
                    yield Optional.ofNullable(securityName)
                            .map(name -> securityRegistrar.declareStockOrBondByName(name, () -> security))
                            .orElseThrow();
                } else {
                    yield securityRegistrar.declareStockOrBondByIsin(securityId, () -> security);
                }
            }
            case DERIVATIVE -> {
                security.ticker(securityId);
                yield Optional.ofNullable(securityId)
                        .map(securityRegistrar::declareDerivative)
                        .orElseThrow();
            }
            case CURRENCY_PAIR -> {
                security.ticker(securityId);
                yield Optional.ofNullable(securityId)
                        .map(securityRegistrar::declareCurrencyPair)
                        .orElseThrow();
            }
            case ASSET -> {
                security.name(securityId);
                yield Optional.ofNullable(securityId)
                        .map(asset -> securityRegistrar.declareAsset(asset, () -> security))
                        .orElseThrow();
            }
        };
        return security.id(id).build();
    }

    /**
     * @param nameAndIsin in format "<name>\s*(<isin>)"
     * @return isin for stock or bond, nameAndIsin for others
     */
    private static @Nullable String getSecurityId(String nameAndIsin, String section) {
        int start = nameAndIsin.indexOf('(') + 1;
        int end = nameAndIsin.indexOf(')');
        String id = nameAndIsin.substring(start, (end == -1) ? nameAndIsin.length() : end);
        if ("Фондовый рынок".equalsIgnoreCase(section)) {
            return getValidIsinOrNull(id);
        }
        return id;
    }

    private static @Nullable String getValidIsinOrNull(String isin) {
        return hasLength(isin) && isinPattern.matcher(isin).matches() ?
                isin :
                null;
    }

    public static String getSecurityName(String nameAndIsin) {
        int start = nameAndIsin.indexOf('(');
        return (start == -1) ? nameAndIsin : nameAndIsin.substring(0, start).trim();
    }

    public static SecurityType getSecurityType(String section, @Nullable String securityType) {
        if ("Фондовый рынок".equalsIgnoreCase(section)) {
            if (securityType == null) return SecurityType.STOCK_OR_BOND;
            return switch (securityType.toLowerCase()) {
                case "акция", "пай", "депозитарная расписка" -> SecurityType.STOCK;
                case "облигация" -> SecurityType.BOND;
                default -> SecurityType.STOCK_OR_BOND;
            };
        } else if (section.toLowerCase().contains("срочный")) { // предположение
            return SecurityType.DERIVATIVE;
        } else if (section.toLowerCase().contains("валютный")) { // предположение
            return SecurityType.CURRENCY_PAIR;
        }
        return SecurityType.ASSET;
    }
}
