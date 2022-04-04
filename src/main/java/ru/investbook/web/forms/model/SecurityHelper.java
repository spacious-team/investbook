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
    public static String NULL_SECURITY_NAME = "<unknown>";

    /**
     * For stock or bond "Name (ISIN)", for derivatives - code, for asset - securityName
     */
    public static String getSecurityDescription(String isin, String securityName, SecurityType securityType) {
        return switch (securityType) {
            case SHARE, BOND -> {
                Assert.isTrue(isin != null || securityName != null, "Отсутствует и ISIN, и наименование ЦБ");
                yield (securityName == null ? NULL_SECURITY_NAME : securityName) +
                        (isin == null ? "" : " (" + isin + ")");
            }
            case DERIVATIVE, CURRENCY, ASSET -> {
                Assert.isTrue(securityName != null, "Отсутствует тикер контракта или наименование произвольного актива");
                yield securityName;
            }
        };
    }

    /**
     * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
     */
    static String getSecurityName(String securityDescription, SecurityType securityType) {
        securityDescription = securityDescription.trim();
        return switch (securityType) {
            case SHARE, BOND -> isSecurityDescriptionHasIsin(securityDescription) ?
                    securityDescription.substring(0, securityDescription.length() - 14) :
                    securityDescription;
            case DERIVATIVE, CURRENCY, ASSET -> securityDescription;
        };
    }

    /**
     * Returns ISIN if description in "Name (ISIN)" format, null otherwise
     */
    static String getSecurityIsin(String securityDescription) {
        securityDescription = securityDescription.trim();
        int len = securityDescription.length();
        return isSecurityDescriptionHasIsin(securityDescription) ?
                securityDescription.substring(len - 13, len - 1) :
                null;
    }

    private static boolean isSecurityDescriptionHasIsin(String securityDescription) {
        securityDescription = securityDescription.trim();
        int len = securityDescription.length();
        return (len >= 15) && securityDescription.charAt(len - 14) == '(' && securityDescription.charAt(len - 1) == ')';
    }
}