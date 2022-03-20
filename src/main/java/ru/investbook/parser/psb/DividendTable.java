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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.psb.DividendTable.DividendTableHeader.*;

@Slf4j
public class DividendTable extends SingleAbstractReportTable<SecurityEventCashFlow> {
    private static final String TABLE_NAME = "Выплата дивидендов";
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public DividendTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, "", DividendTableHeader.class);
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseRowToCollection(TableRow row) {
        String isin = row.getStringCellValue(ISIN);
        int securityId = getReport().getSecurityRegistrar().declareStockByIsin(isin, () -> Security.builder()
                .isin(isin)
                .name(row.getStringCellValue(STOCK_NAME)));
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .security(securityId)
                .portfolio(getReport().getPortfolio())
                .count(row.getIntCellValue(COUNT))
                .eventType(CashFlowType.DIVIDEND)
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .value(row.getBigDecimalCellValue(VALUE))
                .currency(row.getStringCellValue(CURRENCY));
        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());
        BigDecimal tax = row.getBigDecimalCellValue(TAX).negate();
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
        STOCK_NAME("наименование"),
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
