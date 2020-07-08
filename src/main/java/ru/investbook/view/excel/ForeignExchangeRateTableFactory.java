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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.investbook.view.ForeignExchangeRateService;
import ru.investbook.view.Table;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ForeignExchangeRateTableFactory {
    private static final List<String> currencies = Arrays.asList("USD", "EUR", "GBP", "CHF");
    private final ForeignExchangeRateService foreignExchangeRateService;

    public String cashConvertToRubExcelFormula(String currency,
                                               ExcelTableHeader cashColumn,
                                               ExcelTableHeader exchangeRateColumn) {
        int rowNum = currencies.indexOf(currency);
        if (rowNum == -1) {
            return "=" + cashColumn.getCellAddr();
        } else {
            return "=" + cashColumn.getCellAddr() + "*" + exchangeRateColumn.getColumnIndex() + (rowNum + 3); // 2 rows = header + total
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
