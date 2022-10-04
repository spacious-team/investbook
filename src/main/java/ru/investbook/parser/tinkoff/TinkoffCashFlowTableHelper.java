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

package ru.investbook.parser.tinkoff;

import org.spacious_team.table_wrapper.api.TableRow;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.TinkoffCashFlowTable.CashFlowTableHeader.CURRENCY;
import static ru.investbook.parser.tinkoff.TinkoffCashFlowTable.CashFlowTableHeader.DATE;

class TinkoffCashFlowTableHelper {

    static String getCurrency(TableRow row, String defaultCurrency) {
        boolean isCurrencyHeader = !hasLength(row.getStringCellValueOrDefault(DATE, null));
        if (isCurrencyHeader) {
            String currency = row.getStringCellValueOrDefault(CURRENCY, null);
            if (hasLength(currency) && currency.length() == 3) { // RUB, USD, ... (ISO format)
                return currency.toUpperCase();
            }
        }
        return defaultCurrency;
    }
}
