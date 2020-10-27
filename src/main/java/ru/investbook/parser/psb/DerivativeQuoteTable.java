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

package ru.investbook.parser.psb;

import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.pojo.SecurityQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;

public class DerivativeQuoteTable extends AbstractReportTable<SecurityQuote> {

    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public DerivativeQuoteTable(PsbBrokerReport report) {
        super(report, DerivativeCashFlowTable.TABLE2_NAME, DerivativeCashFlowTable.TABLE_END_TEXT,
                DerivativeCashFlowTable.ContractCountTableHeader.class);
    }

    @Override
    protected Collection<SecurityQuote> getRow(Table table, TableRow row) {
        BigDecimal price = table.getCurrencyCellValue(row, PRICE);
        BigDecimal tickValue = table.getCurrencyCellValue(row, PRICE_TICK_VALUE);
        if (price.compareTo(minValue) < 0 || tickValue.compareTo(minValue) < 0) {
            return emptyList();
        }
        BigDecimal tick = table.getCurrencyCellValue(row, PRICE_TICK);
        BigDecimal quote = price.multiply(tick)
                .divide(tickValue, 2, RoundingMode.HALF_UP);
        return Collections.singletonList(SecurityQuote.builder()
                .isin(table.getStringCellValue(row, CONTRACT))
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .build());
    }
}
