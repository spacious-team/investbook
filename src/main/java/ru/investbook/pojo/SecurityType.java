/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.pojo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SecurityType {

    STOCK_OR_BOND("акция/облигация"),
    DERIVATIVE("срочный контракт"),
    CURRENCY_PAIR("валюта");

    @Getter
    private final String description;

    public static SecurityType getSecurityType(Security security) {
        return getSecurityType(security.getIsin());
    }

    public static SecurityType getSecurityType(String isin) {
        if (isin.endsWith("_TOD") || isin.endsWith("_TOM") || isin.length() == 6) { // USDRUB_TOM or USDRUB_TOD or USDRUB
            return CURRENCY_PAIR;
        } else if (isin.length() == 12) {
            return STOCK_OR_BOND;
        } else {
            return DERIVATIVE;
        }
    }

    /**
     * Returns currency pairs, for example USDRUB, EURRUB
     */
    public static String getCurrencyPair(String contract) {
        return (contract.length() == 6) ? contract :
                contract.replace("_TOD", "")
                        .replace("_TOM", "");
    }
}
