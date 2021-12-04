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

package ru.investbook.web.forms.model;

import org.springframework.util.Assert;

public class SecurityHelper {

    /**
     * For stock or bond "Name (ISIN)", for derivatives - securityId, for asset - securityName
     */
    public static String getSecurityDescription(String securityId, String securityName, SecurityType securityType) {
        return switch (securityType) {
            case SHARE, BOND -> securityName + " (" + securityId + ")";
            case DERIVATIVE, CURRENCY -> securityId;
            case ASSET -> securityName;
        };
    }

    /**
     * Returns ISIN from template "Name (ISIN)" for stock and bond, securityId for derivative, null for asset
     */
    static String getSecurityId(String securityDescription, SecurityType securityType) {
        return switch (securityType) {
            case SHARE, BOND -> {
                Assert.isTrue(isSecurityDescriptionHasIsin(securityDescription),
                        "Не задан ISIN: " + securityDescription);
                int len = securityDescription.length();
                yield securityDescription.substring(len - 13, len - 1);
            }
            case DERIVATIVE, CURRENCY -> securityDescription;
            case ASSET -> null;
        };
    }

    /**
     * Returns Name from template "Name (ISIN)" for stock and bond, null for derivative, securityName for asset
     */
    static String getSecurityName(String securityDescription, SecurityType securityType) {
        return switch (securityType) {
            case SHARE, BOND -> {
                Assert.isTrue(isSecurityDescriptionHasIsin(securityDescription),
                        "Не задан ISIN: " + securityDescription);
                yield securityDescription.substring(0, securityDescription.length() - 14).trim();
            }
            case DERIVATIVE, CURRENCY -> null;
            case ASSET -> securityDescription;
        };
    }

    /**
     * For stock or bond "Name" without ISIN
     */
    static String getSecurityDisplayName(String securityDescription, SecurityType securityType) {
        String name = SecurityHelper.getSecurityName(securityDescription, securityType);
        return (name == null) ? securityDescription : name;
    }

    private static boolean isSecurityDescriptionHasIsin(String securityDescription) {
        int len = securityDescription.length();
        return (len >= 15) && securityDescription.charAt(len - 14) == '(' && securityDescription.charAt(len - 1) == ')';
    }
}