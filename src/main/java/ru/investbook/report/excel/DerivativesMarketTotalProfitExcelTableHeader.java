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

package ru.investbook.report.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DerivativesMarketTotalProfitExcelTableHeader implements ExcelTableHeader {
    CONTRACT_GROUP("Группа контрактов"),
    FIRST_TRANSACTION_DATE("Дата первой сделки"),
    LAST_TRANSACTION_DATE("Дата последней сделки"),
    BUY_COUNT("Куплено контрактов"),
    CELL_COUNT("Продано контрактов"),
    COUNT("Открыто позиций"),
    COMMISSION("Комиссия"),
    LAST_EVENT_DATE("Дата последней выплаты"),
    GROSS_PROFIT_PNT("Курсовая разница, пункты"),
    GROSS_PROFIT("Вариационная маржа"),
    PROFIT("Прибыль"),
    PROFIT_PROPORTION("Доля прибыли, %");

    private final String description;
}
