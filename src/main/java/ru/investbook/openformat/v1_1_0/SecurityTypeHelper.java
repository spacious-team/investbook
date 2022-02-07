/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.openformat.v1_1_0;

import org.spacious_team.broker.pojo.SecurityType;

public class SecurityTypeHelper {

    public static SecurityType getSecurityType(String openPortfolioFormatType) {
        return switch (openPortfolioFormatType) {
            case "stock" -> SecurityType.STOCK;
            case "bond" -> SecurityType.BOND;
            case "security" -> SecurityType.STOCK_OR_BOND;
            case "derivative" -> SecurityType.DERIVATIVE;
            case "fx-contract" -> SecurityType.CURRENCY_PAIR;
            default -> SecurityType.ASSET; // asset
        };
    }

    public static String toPofType(SecurityType securityType) {
        return switch (securityType) {
            case STOCK -> "stock";
            case BOND -> "bond";
            case STOCK_OR_BOND -> "security";
            case CURRENCY_PAIR -> "fx-contract";
            case DERIVATIVE -> "derivative";
            case ASSET -> "asset";
        };
    }

}
