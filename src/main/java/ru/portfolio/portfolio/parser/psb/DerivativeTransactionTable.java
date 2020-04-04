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

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends AbstractReportTable<DerivativeTransactionTable.FortsTableRow> {
    public static final String QUOTE_CURRENCY = "PNT"; // point
    private static final String TABLE_NAME = "Информация о заключенных сделках";
    private static final String TABLE_END_TEXT = "Итого";

    public DerivativeTransactionTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, FortsTableHeader.class);
        getData().addAll(new DerivativeExpirationTable(report).getData());
    }

    @Override
    protected Collection<FortsTableRow> getRow(ExcelTable table, Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        int count = table.getIntCellValue(row, COUNT);
        String type = table.getStringCellValue(row, TYPE).toLowerCase();
        BigDecimal value;
        BigDecimal valueInPoints;
        switch (type) {
            case "опцион":
                value = table.getCurrencyCellValue(row, OPTION_PRICE).multiply(BigDecimal.valueOf(count));
                valueInPoints = table.getCurrencyCellValue(row, OPTION_QUOTE).multiply(BigDecimal.valueOf(count));
                break;
            case "фьючерс":
                value = table.getCurrencyCellValue(row, VALUE);
                valueInPoints = table.getCurrencyCellValue(row, QUOTE).multiply(BigDecimal.valueOf(count));
                break;
            default:
                throw new IllegalArgumentException("Не известный контракт " + type);
        }
        if (isBuy) {
            value = value.negate();
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        List<FortsTableRow> transactionInfo = new ArrayList<>(2);
        FortsTableRow.FortsTableRowBuilder builder = FortsTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(Long.parseLong(table.getStringCellValue(row, TRANSACTION)))
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

    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата включения в клиринг"),
        TRANSACTION("№"),
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

    @Getter
    @Builder
    @EqualsAndHashCode
    static class FortsTableRow {
        private long transactionId;
        private String contract;
        private Instant timestamp;
        private int count;
        private BigDecimal value; // оценочная стоиомсть в валюце цены
        private BigDecimal commission;
        private String currency; // валюта
    }
}
