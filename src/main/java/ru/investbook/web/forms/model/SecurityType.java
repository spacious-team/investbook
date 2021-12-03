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

public enum SecurityType {
    SHARE, BOND, DERIVATIVE, CURRENCY, ASSET;

    public static SecurityType valueOf(org.spacious_team.broker.pojo.SecurityType type) {
        return switch (type) {
            case STOCK, STOCK_OR_BOND -> SHARE;
            case BOND -> BOND;
            case DERIVATIVE -> DERIVATIVE;
            case CURRENCY_PAIR -> CURRENCY;
            case ASSET -> ASSET;
        };
    }

    public String getDescription() {
        return switch (this) {
            case SHARE -> "акция";
            case BOND -> "облигация";
            case DERIVATIVE -> "срочный контракт";
            case CURRENCY -> "валютная пара";
            case ASSET -> "произвольный актив";
        };
    }
}
