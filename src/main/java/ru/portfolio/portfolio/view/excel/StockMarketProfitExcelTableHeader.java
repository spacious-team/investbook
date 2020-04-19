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
public enum StockMarketProfitExcelTableHeader implements ExcelTableHeader {
    SECURITY("Бумага"),
    BUY_DATE("Дата покупки"),
    COUNT("Количество"),
    BUY_PRICE("Цена"),
    BUY_AMOUNT("Стоимость (без НКД и комиссии)"),
    BUY_ACCRUED_INTEREST("НКД уплаченный"),
    BUY_COMMISSION("Комиссия покупки"),
    CELL_DATE("Дата продажи"),
    CELL_AMOUNT("Стоимость продажи/погашения"),
    CELL_ACCRUED_INTEREST("НКД при продаже" ),
    COUPON("Выплачены купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    CELL_COMMISSION("Комиссия продажи/погашения"),
    TAX("Налог с купонов и дивидендов (уплаченный)"),
    FORECAST_TAX("Налог с разницы курсов (ожидаемый)"),
    PROFIT("Доходность годовых, %");

    private final String description;
}
