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

package ru.investbook.openformat;

import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.Objects;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.entity.SecurityEntity.isinPattern;

public class OpenFormatHelper {

    public static @PolyNull String getValidCurrencyOrNull(@PolyNull String currency) {
        if (currency == null) return null;
        currency = currency.toUpperCase();
        return Objects.equals(currency, "RUR") ? "RUB" : currency;
    }

    public static @PolyNull String getValidIsinOrNull(@PolyNull String isin) {
        return hasLength(isin) && isinPattern.matcher(isin).matches() ?
                isin :
                null;
    }
}
