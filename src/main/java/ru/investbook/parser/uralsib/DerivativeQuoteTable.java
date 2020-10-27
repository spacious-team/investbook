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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import org.spacious_team.table_wrapper.api.*;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.pojo.SecurityQuote;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.DerivativeQuoteTable.ContractCountTableHeader.CONTRACT;
import static ru.investbook.parser.uralsib.DerivativeQuoteTable.ContractCountTableHeader.QUOTE;

public class DerivativeQuoteTable extends AbstractReportTable<SecurityQuote> {

    private static final String TABLE_NAME = "Движение стандартных контрактов";
    private static final String TABLE_END_TEXT = "Итого:";
    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    protected DerivativeQuoteTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, DerivativeQuoteTable.ContractCountTableHeader.class);
    }

    @Override
    protected Collection<SecurityQuote> getRow(Table table, TableRow row) {
        BigDecimal quote = table.getCurrencyCellValue(row, QUOTE);
        if (quote.compareTo(minValue) < 0) {
            return emptyList();
        }
        return Collections.singletonList(SecurityQuote.builder()
                .isin(table.getStringCellValue(row, CONTRACT))
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .build());
    }

    enum ContractCountTableHeader implements TableColumnDescription {
        CONTRACT("наименование контракта"),
        INCOMING("входящий остаток"),
        OUTGOING("исходящий остаток"),
        BUY("зачислено"),
        CELL("списано"),
        QUOTE("расчетная цена");

        @Getter
        private final TableColumn column;
        ContractCountTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
