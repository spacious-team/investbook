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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.parser.TableColumn;
import ru.investbook.parser.TableColumnDescription;
import ru.investbook.parser.TableColumnImpl;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.SecurityEventCashFlow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.psb.DividendTable.DividendTableHeader.*;

@Slf4j
public class DividendTable extends AbstractReportTable<SecurityEventCashFlow> {
    private static final String TABLE_NAME = "Выплата дивидендов";
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public DividendTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, "", DividendTableHeader.class);
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(Table table, TableRow row) {
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .isin(table.getStringCellValue(row, ISIN))
                .portfolio(getReport().getPortfolio())
                .count(table.getIntCellValue(row, COUNT))
                .eventType(CashFlowType.DIVIDEND)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(table.getStringCellValue(row, CURRENCY));
        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());
        BigDecimal tax = table.getCurrencyCellValue(row, TAX).negate();
        if (tax.abs().compareTo(minValue) >= 0) {
            data.add(builder
                    .eventType(CashFlowType.TAX)
                    .value(tax)
                    .build());
        }
        return data;
    }

    enum DividendTableHeader implements TableColumnDescription {
        DATE("дата"),
        ISIN("isin"),
        COUNT("кол-во"),
        VALUE("сумма", "дивидендов"),
        CURRENCY("валюта", "выплаты"),
        TAX("сумма", "налога");

        @Getter
        private final TableColumn column;
        DividendTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
