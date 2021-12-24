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

package ru.investbook.parser.sber;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import ru.investbook.parser.SecurityRegistrar;


@Slf4j
public class SecurityHelper {

    public static Security getSecurity(String code, String securityName, String section, String type, SecurityRegistrar securityRegistrar) {
        String securityId = getSecurityId(code, section);
        SecurityType securityType = getSecurityType(section, type);
        Security.SecurityBuilder security = Security.builder().type(securityType);
        int id = -1;
        switch (securityType) {
            case STOCK, BOND, STOCK_OR_BOND -> {
                security
                        .isin(securityId)
                        .name(securityName);
                id = securityRegistrar.declareStockOrBond(securityId, () -> security);
            }
            case DERIVATIVE -> {
                security.ticker(securityId);
                id = securityRegistrar.declareDerivative(securityId);
            }
            case CURRENCY_PAIR -> {
                security.ticker(securityId);
                id = securityRegistrar.declareCurrencyPair(securityId);
            }
            case ASSET -> {
                security.name(securityId);
                id = securityRegistrar.declareAsset(securityId, () -> security);
            }
        }
        return security.id(id).build();
    }

    /**
     * @param nameAndIsin in format "<name>\s*(<isin>)"
     * @return isin for stock or bond, nameAndIsin for others
     */
    private static String getSecurityId(String nameAndIsin, String section) {
        int start = nameAndIsin.indexOf('(') + 1;
        int end = nameAndIsin.indexOf(')');
        String id = nameAndIsin.substring(start, (end == -1) ? nameAndIsin.length() : end);
        if (id.length() != 12 && "Фондовый рынок".equalsIgnoreCase(section)) {
            if (id.length() > 12) {
                log.warn("Код инструмента '{}' фондового рынка более 12 символов, обрезаю, " +
                        "отредактируйте ISIN через API", id);
                id = id.substring(0, 12);
            } else {
                log.warn("Код инструмента '{}' фондового рынка менее 12 символов, дополняю справа знаками '0', " +
                        "отредактируйте ISIN через API", id);
                id += "0".repeat(12 - id.length());
            }
        }
        return id;
    }

    public static String getSecurityName(String nameAndIsin) {
        int start = nameAndIsin.indexOf('(');
        return (start == -1) ? nameAndIsin : nameAndIsin.substring(0, start).trim();
    }

    public static SecurityType getSecurityType(String section, String securityType) {
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
