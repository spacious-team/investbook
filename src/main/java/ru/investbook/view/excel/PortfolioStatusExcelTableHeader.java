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
public enum PortfolioStatusExcelTableHeader implements ExcelTableHeader {
    SECURITY("Бумага"),
    FIRST_TRNSACTION_DATE("Дата первой сделки"),
    LAST_TRANSACTION_DATE("Дата последней сделки"),
    BUY_COUNT("Куплено"),
    CELL_COUNT("Продано"),
    COUNT("Текущая позиция"),
    AVERAGE_PRICE("Усредненная цена"),
    AVERAGE_ACCRUED_INTEREST("Усредненный НКД"),
    COMMISSION("Комиссия"),
    LAST_EVENT_DATE("Дата последней выплаты"),
    COUPON("Выплаченные купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    TAX("Налог (удержанный)"),
    GROSS_PROFIT("Курсовой доход"),
    PROFIT("Прибыль"),
    PROPORTION("Доля, %");

    private final String description;
}
