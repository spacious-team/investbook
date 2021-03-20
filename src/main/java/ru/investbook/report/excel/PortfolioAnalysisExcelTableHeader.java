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
public enum PortfolioAnalysisExcelTableHeader implements ExcelTableHeader {
    DATE("Дата"),
    INVESTMENT_AMOUNT("Инвестиция"),
    INVESTMENT_CURRENCY("Валюта инвестиции"),
    INVESTMENT_AMOUNT_USD("Инвестиция, USD"),
    TOTAL_INVESTMENT_USD("Сумма инвестиций, USD"),
    CASH_RUB("Остаток ДС, руб"),
    CASH_USD("Остаток ДС, USD"),
    CASH_EUR("Остаток ДС, EUR"),
    CASH_GBP("Остаток ДС, GBP"),
    CASH_CHF("Остаток ДС, CHF"),
    TOTAL_CASH_USD("Итого остаток ДС, USD"),
    ASSETS_RUB("Активы, руб"),
    ASSETS_USD("Активы, USD"),
    ASSETS_GROWTH("Рост активов, %"),
    SP500("Индекс S&P 500"),
    SP500_GROWTH("Рост S&P 500, %"),
    CURRENCY_NAME("Валюта"),
    EXCHANGE_RATE("Курс, руб");

    private final String description;
}
