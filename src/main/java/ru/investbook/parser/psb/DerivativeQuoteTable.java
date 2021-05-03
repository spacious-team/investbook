/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static ru.investbook.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;

public class DerivativeQuoteTable extends SingleAbstractReportTable<SecurityQuote> {

    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public DerivativeQuoteTable(PsbBrokerReport report) {
        super(report, DerivativeCashFlowTable.TABLE2_NAME, DerivativeCashFlowTable.TABLE_END_TEXT,
                DerivativeCashFlowTable.ContractCountTableHeader.class);
    }

    @Override
    protected SecurityQuote parseRow(TableRow row) {
        BigDecimal price = row.getBigDecimalCellValue(PRICE);
        BigDecimal tickValue = row.getBigDecimalCellValue(PRICE_TICK_VALUE);
        if (price.compareTo(minValue) < 0 || tickValue.compareTo(minValue) < 0) {
            return null;
        }
        BigDecimal tick = row.getBigDecimalCellValue(PRICE_TICK);
        BigDecimal quote = price.multiply(tick)
                .divide(tickValue, 2, RoundingMode.HALF_UP);
        return SecurityQuote.builder()
                .security(row.getStringCellValue(CONTRACT))
                .timestamp(getReport().getReportEndDateTime())
                .quote(quote)
                .price(price)
                .build();
    }
}
