/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.ForeignExchangeTransactionTable.FxTransactionTableHeader.*;

@Slf4j
public class ForeignExchangeTransactionTable extends SingleAbstractReportTable<ForeignExchangeTransaction> {
    private static final String TABLE_NAME = "Биржевые валютные сделки, совершенные в отчетном периоде";
    private static final String CONTRACT_PREFIX = "Инструмент:";
    private String instrument = null;

    public ForeignExchangeTransactionTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", FxTransactionTableHeader.class, 2);
    }

    @Override
    protected Collection<ForeignExchangeTransaction> parseRowToCollection(TableRow row) {
        String transactionId;
        Object cellValue = row.getCellValue(TRANSACTION);
        if (cellValue instanceof String) {
            String stringValue = cellValue.toString();
            try {
                // some numbers (doubles) represented by string type cells
                transactionId = String.valueOf(Long.parseLong(stringValue));
            } catch (NumberFormatException e) {
                if (stringValue.startsWith(CONTRACT_PREFIX)) {
                    instrument = stringValue.substring(CONTRACT_PREFIX.length()).trim();
                }
                return emptyList();
            }
        } else if (cellValue instanceof Number) {
            // double
            transactionId = String.valueOf(((Number) cellValue).longValue());
        } else {
            return emptyList();
        }
        if (instrument == null || instrument.isEmpty()) {
            return emptyList();
        }

        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isBuy) {
            value = value.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .negate();

        return Collections.singletonList(ForeignExchangeTransaction.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE_TIME)))
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .security(instrument)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .commission(commission)
                .valueCurrency(UralsibBrokerReport.convertToCurrency(row.getStringCellValue(VALUE_CURRENCY)))
                .commissionCurrency("RUB")
                .build());
    }

    enum FxTransactionTableHeader implements TableColumnDescription {
        DATE_TIME("дата", "сделки"), // не "дата исполнения", иначе не примутся в расчет сделки выполненные без обналичивания валюты
        TRANSACTION("номер сделки"),
        DIRECTION("вид", "сделки"),
        COUNT("кол-во","шт"),
        VALUE("сумма сопряженной валюты"),
        VALUE_CURRENCY("сопряженная валюта"),
        MARKET_COMMISSION("комиссия tc", "руб"),
        BROKER_COMMISSION("комиссия брокера", "руб");

        @Getter
        private final TableColumn column;
        FxTransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
