/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CashFlowExcelTableHeader implements ExcelTableHeader {
    DATE("Дата"),
    CASH("Внесено (+) / Выведено (-)"),
    CURRENCY("Валюта"),
    CASH_RUB("Внесено (+) / Выведено (-), руб"),
    DAYS_COUNT("Количество дней на счету"),
    DESCRIPTION("Описание"),
    LIQUIDATION_VALUE_RUB("Ликвидная стоимость, руб"),
    PROFIT("Доходность годовых, %"),
    CURRENCY_NAME("Валюта"),
    EXCHANGE_RATE("Курс, руб");

    private final String description;
}
