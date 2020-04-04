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

package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.DerivativeExpirationTable.ExpirationTableHeader.*;
import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.QUOTE_CURRENCY;

@Slf4j
class DerivativeExpirationTable extends AbstractReportTable<DerivativeTransactionTable.FortsTableRow> {
    private static final String TABLE_NAME = "Исполнение контрактов";
    private static final String TABLE_END_TEXT = "Итого";

    DerivativeExpirationTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, ExpirationTableHeader.class);
    }

    @Override
    protected Collection<DerivativeTransactionTable.FortsTableRow> getRow(ExcelTable table, Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        int count = table.getIntCellValue(row, COUNT);
        String type = table.getStringCellValue(row, TYPE).toLowerCase();
        BigDecimal value;
        BigDecimal valueInPoints;
        switch (type) {
            case "фьючерс":
                value = table.getCurrencyCellValue(row, VALUE);
                valueInPoints = table.getCurrencyCellValue(row, QUOTE).multiply(BigDecimal.valueOf(count));
                break;
            case "опцион":
                value = valueInPoints = BigDecimal.ZERO;
                break;
            default:
                throw new IllegalArgumentException("Не известный контракт '" + type + "'"); // unexpected contract
        }
        if (isBuy) {
            value = value.negate();
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        List<DerivativeTransactionTable.FortsTableRow> transactionInfo = new ArrayList<>(2);
        DerivativeTransactionTable.FortsTableRow.FortsTableRowBuilder builder =
                DerivativeTransactionTable.FortsTableRow.builder()
                        .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                        .transactionId(table.getLongCellValue(row, TRANSACTION))
                        .contract(table.getStringCellValue(row, CONTRACT))
                        .count((isBuy ? 1 : -1) * count);
        transactionInfo.add(builder
                .value(value)
                .commission(commission)
                .currency("RUB") // FORTS, only RUB
                .build());
        transactionInfo.add(builder
                .value(valueInPoints)
                .commission(BigDecimal.ZERO)
                .currency(QUOTE_CURRENCY)
                .build());
        return transactionInfo;
    }

    enum ExpirationTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        QUOTE("цена", "пункты"),
        VALUE("сумма"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        ExpirationTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
