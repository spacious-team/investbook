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
import org.spacious_team.broker.pojo.SecurityType;


@Slf4j
public class SecurityHelper {

    /**
     * @param nameAndIsin in format "<name>\s*(<isin>)"
     * @return isin
     */
    public static String getSecurityId(String nameAndIsin, String section) {
        int start = nameAndIsin.indexOf('(') + 1;
        int end = nameAndIsin.indexOf(')');
        String id = nameAndIsin.substring(start, (end == -1) ? nameAndIsin.length() : end);
        if (id.length() != 12 && "Фондовый рынок".equalsIgnoreCase(section)) {
            if (id.length() > 12) {
                log.warn("Код инструмента '{}' фондового рынка более 12 символов, обрезаю, " +
                        "отредактируйте ISIN через API", id);
                id = id.substring(0, 12);
            } else {
                log.warn("Код инструмента '{}' фондового рынка менее 12 символов, дополняю справа знаками '_', " +
                        "отредактируйте ISIN через API", id);
                id += "_".repeat(12 - id.length());
            }

        }
        return id;
    }

    public static String getSecurityName(String nameAndIsin) {
        int start = nameAndIsin.indexOf('(');
        return (start == -1) ? null : nameAndIsin.substring(0, start).trim();
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
