/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
public enum StockMarketProfitExcelTableHeader implements ExcelTableHeader {
    SECURITY("Бумага"),
    OPEN_DATE("Дата открытия"),
    COUNT("Количество"),
    OPEN_PRICE("Цена"),
    OPEN_AMOUNT("Стоимость (без НКД и комиссии)"),
    OPEN_ACCRUED_INTEREST("НКД уплаченный"),
    OPEN_COMMISSION("Комиссия открытия"),
    CLOSE_DATE("Дата закрытия"),
    CLOSE_AMOUNT("Стоимость закрытия/погашения"),
    CLOSE_ACCRUED_INTEREST("НКД при закрытии" ),
    COUPON("Выплаченные купоны"),
    AMORTIZATION("Амортизация облигации"),
    DIVIDEND("Дивиденды"),
    CLOSE_COMMISSION("Комиссия закрытия/погашения"),
    TAX("Налог с купонов и дивидендов (уплаченный)"),
    TAX_LIABILITY("Налог к уплате с дивидендов и купонов"),
    FORECAST_TAX("Налог с разницы курсов (ожидаемый)"),
    PROFIT("Прибыль (закрытых позиций)"),
    YIELD("Доходность годовых, %");

    private final String description;
}
