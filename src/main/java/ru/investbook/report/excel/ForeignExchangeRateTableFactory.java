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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.investbook.report.ForeignExchangeRateService;
import ru.investbook.report.Table;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForeignExchangeRateTableFactory {
    private final List<String> currencies = Arrays.asList("USD", "EUR", "GBP", "CHF");
    private final int FIRST_CURRENCY_ROW = 3; // 2 rows = header + total
    private final ForeignExchangeRateService foreignExchangeRateService;

    public String cashConvertToUsdExcelFormula(String currency,
                                               ExcelTableHeader cashColumn,
                                               ExcelTableHeader exchangeRateColumn) {
        if (currency.equalsIgnoreCase("USD")) {
            return "=" + cashColumn.getCellAddr();
        } else {
            String toRub = cashConvertToRubExcelFormula(currency, cashColumn, exchangeRateColumn);
            return toRub + "/" + exchangeRateColumn.getColumnIndex() + FIRST_CURRENCY_ROW; // USD is a first currency
        }
    }

    public String cashConvertToRubExcelFormula(String currency,
                                               ExcelTableHeader cashColumn,
                                               ExcelTableHeader exchangeRateColumn) {
        int currencyIndex = currencies.indexOf(currency);
        if (currencyIndex == -1) {
            if (!currency.equalsIgnoreCase("RUB")) {
                log.warn("Не могу конвертировать курс валюты {} в RUB", currency);
            }
            return "=" + cashColumn.getCellAddr();
        } else {
            return "=" + cashColumn.getCellAddr() + "*" +
                    exchangeRateColumn.getColumnIndex() + (FIRST_CURRENCY_ROW + currencyIndex);
        }
    }

    public void appendExchangeRates(Table table,
                                    ExcelTableHeader currencyNameColumn,
                                    ExcelTableHeader exchangeRateColumn) {
        for (int i = 0; i < currencies.size(); i++) {
            String currency = currencies.get(i);
            if (table.size() <= i) table.add(new Table.Record());
            Table.Record record = table.get(i);
            record.put(currencyNameColumn, currency);
            record.put(exchangeRateColumn, foreignExchangeRateService.getExchangeRateToRub(currency));
        }
    }
}
