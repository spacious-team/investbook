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

package ru.investbook.report.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DerivativesMarketProfitExcelTableHeader implements ExcelTableHeader {
    CONTRACT("Контракт"),
    DIRECTION("Сделка"),
    DATE("Дата"),
    COUNT("Количество"),
    QUOTE("Котировка / премия, пункты"),
    AMOUNT("Сумма, руб"),
    COMMISSION("Коммиссия, руб"),
    DERIVATIVE_PROFIT_DAY("Дневная вариационная маржа, руб"),
    DERIVATIVE_PROFIT_TOTAL("Накопленная вариационная маржа, руб"),
    POSITION("Позиция на конец дня"),
    FORECAST_TAX("Ожидаемый налог, руб"),
    PROFIT("Прибыль, руб");

    private final String description;
}
