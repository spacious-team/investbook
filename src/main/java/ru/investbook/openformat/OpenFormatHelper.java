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

package ru.investbook.openformat;

import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

public class OpenFormatHelper {

    private static final Pattern isinPattern = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");

    public static String getValidCurrencyOrNull(String currency) {
        if (currency == null) return null;
        currency = currency.toUpperCase();
        return Objects.equals(currency, "RUR") ? "RUB" : currency;
    }

    public static String getValidIsinOrNull(String isin) {
        return StringUtils.hasLength(isin) && isinPattern.matcher(isin).matches() ?
                isin :
                null;
    }
}
