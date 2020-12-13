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

package ru.investbook.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PortfolioAnalysisExcelTableHeader implements ExcelTableHeader {
    DATE("Дата"),
    INVESTMENT_AMOUNT("Инвестиция"),
    INVESTMENT_CURRENCY("Валюта инвестиции"),
    INVESTMENT_AMOUNT_USD("Инвестиция, usd"),
    TOTAL_INVESTMENT_USD("Сумма инвестиций, usd"),
    CASH_RUB("Остаток ДС, руб"),
    CASH_USD("Остаток ДС, usd"),
    CASH_EUR("Остаток ДС, eur"),
    CASH_GBP("Остаток ДС, фунт"),
    CASH_CHF("Остаток ДС, франк"),
    TOTAL_CASH_USD("Итого остаток ДС, usd"),
    ASSETS_RUB("Активы, руб"),
    ASSETS_USD("Активы, usd"),
    ASSETS_GROWTH("Рост активов, %"),
    SP500("Индекс S&P 500"),
    SP500_GROWTH("Рост S&P 500, %"),
    CURRENCY_NAME("Валюта"),
    EXCHANGE_RATE("Курс, руб");

    private final String description;
}
