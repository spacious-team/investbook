/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.model.dto;

class SecurityHelper {

    /**
     * Returns ISIN from template "Name (ISIN)"
     */
    static String getSecurityId(String securityDescription) {
        if (SecurityHelper.isSecurityDescriptionHasIsin(securityDescription)) {
            int len = securityDescription.length();
            return securityDescription.substring(len - 13, len - 1);
        }
        return securityDescription;
    }

    /**
     * Returns Name from template "Name (ISIN)"
     */
    static String getSecurityName(String securityDescription) {
        if (SecurityHelper.isSecurityDescriptionHasIsin(securityDescription)) {
            return securityDescription.substring(0, securityDescription.length() - 14).trim();
        }
        return null;
    }

    static boolean isSecurityDescriptionHasIsin(String securityDescription) {
        int len = securityDescription.length();
        return (len >= 15) && securityDescription.charAt(len - 14) == '(' && securityDescription.charAt(len - 1) == ')';
    }
}