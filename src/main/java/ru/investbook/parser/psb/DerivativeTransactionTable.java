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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.psb.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends SingleAbstractReportTable<DerivativeTransaction> {
    private static final String TABLE_NAME = "Информация о заключенных сделках";
    private static final String TABLE_END_TEXT = "Итого";

    public DerivativeTransactionTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, FortsTableHeader.class);
    }

    @Override
    protected Collection<DerivativeTransaction> parseTable(Table table) {
        Collection<DerivativeTransaction> data = new ArrayList<>();
        data.addAll(super.parseTable(table));
        data.addAll(new DerivativeExpirationTable((PsbBrokerReport) getReport()).getData());
        return data;
    }

    @Override
    protected DerivativeTransaction parseRow(TableRow row) {
        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        int count = row.getIntCellValue(COUNT);
        String type = row.getStringCellValue(TYPE).toLowerCase();
        BigDecimal value;
        BigDecimal valueInPoints;
        switch (type) {
            case "опцион" -> {
                value = row.getBigDecimalCellValue(OPTION_PRICE).multiply(BigDecimal.valueOf(count));
                valueInPoints = row.getBigDecimalCellValue(OPTION_QUOTE).multiply(BigDecimal.valueOf(count));
            }
            case "фьючерс" -> {
                value = row.getBigDecimalCellValue(VALUE);
                valueInPoints = row.getBigDecimalCellValue(QUOTE).multiply(BigDecimal.valueOf(count));
            }
            default -> throw new IllegalArgumentException("Не известный контракт " + type);
        }
        if (isBuy) {
            value = value.negate();
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .negate();
        String contract = row.getStringCellValue(CONTRACT);
        int securityId = getReport().getSecurityRegistrar().declareDerivative(contract);
        return DerivativeTransaction.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE_TIME)))
                .tradeId(String.valueOf(row.getLongCellValue(TRADE_ID))) // double numbers
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * count)
                .valueInPoints(valueInPoints)
                .value(value)
                .commission(commission)
                .valueCurrency("RUB") // FORTS, only RUB
                .commissionCurrency("RUB") // FORTS, only RUB
                .build();
    }

    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата включения в клиринг"),
        TRADE_ID("№"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        QUOTE("цена фьючерсного контракта", "цена исполнения опциона", "пункты"),
        VALUE("сумма срочной сделки"),
        OPTION_QUOTE("цена опциона", "пункты"),
        OPTION_PRICE("цена опциона", "руб"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

}
