/*
 * Portfolio
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

package ru.portfolio.portfolio.view.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PortfolioStatusExcelTableHeader implements ExcelTableHeader {
    SECURITY("Бумага"),
    FIRST_TRNSACTION_DATE("Дата первой сделки"),
    LAST_TRANSACTION_DATE("Дата последней сделки"),
    LAST_EVENT_DATE("Дата последнего события"),
    BUY_COUNT("Всего куплено"),
    CELL_COUNT("Всего продано"),
    COUNT("Текущая позиция"),
    AVERAGE_PRICE("Усредненная цена ЦБ"),
    COMMISSION("Комиссии"),
    COUPON("Выплаченные купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    TAX("Налог с купонов и дивидендов (уплаченный)"),
    PROFIT("Прибыль");

    private final String description;
}
