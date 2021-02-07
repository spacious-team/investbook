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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends AbstractReportTable<DerivativeTransaction> {
    private static final String TABLE_NAME = "СДЕЛКИ С ФЬЮЧЕРСАМИ И ОПЦИОНАМИ";
    private static final String TABLE_END_TEXT = PaymentsTable.TABLE_NAME;
    private boolean expirationTableReached = false;

    public DerivativeTransactionTable(UralsibBrokerReport report) {
        this(report, TABLE_NAME, TABLE_END_TEXT, 2);
    }

    protected DerivativeTransactionTable(UralsibBrokerReport report, String tableName, String tableFooter, int headersRowCount) {
        super(report, tableName, tableFooter, FortsTableHeader.class, headersRowCount);
    }

    @Override
    protected Collection<DerivativeTransaction> getRow(Table table, TableRow row) {
        if (expirationTableReached) return emptyList();
        String transactionId = SecurityTransactionTable.getTransactionId(table, row, TRANSACTION);
        if (transactionId == null) {
            if (DerivativeExpirationTable.TABLE_NAME.equals(table.getStringCellValueOrDefault(row, TRANSACTION, null))) {
                expirationTableReached = true;
            }
            return emptyList();
        }

        String direction = table.getStringCellValue(row, DIRECTION);
        boolean isBuy = direction.equalsIgnoreCase("покупка") || direction.equalsIgnoreCase("зачисление");
        int count = table.getIntCellValue(row, COUNT);
        BigDecimal valueInPoints = table.getCurrencyCellValue(row, QUOTE).multiply(BigDecimal.valueOf(count));
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        String valueCurrency = UralsibBrokerReport.convertToCurrency(
                table.getStringCellValueOrDefault(row, VALUE_CURRENCY, "RUB"));
        if (isBuy) {
            value = value.negate();
            valueInPoints = valueInPoints.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .add(table.getCurrencyCellValueOrDefault(row, CLEARING_COMMISSION, BigDecimal.ZERO))
                .negate();
        return Collections.singleton(DerivativeTransaction.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .security(table.getStringCellValue(row, CONTRACT))
                .count((isBuy ? 1 : -1) * count)
                .valueInPoints(valueInPoints)
                .value(value)
                .commission(commission)
                .valueCurrency(valueCurrency)
                .commissionCurrency("RUB") // FORTS, only RUB
                .build());
    }

    @RequiredArgsConstructor
    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME(AnyOfTableColumn.of(
                TableColumnImpl.of("дата расчетов"),
                TableColumnImpl.of("дата исполнения"))),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("наименование контракта"),
        DIRECTION(AnyOfTableColumn.of(
                TableColumnImpl.of("вид сделки"),
                TableColumnImpl.of("зачисление/списание"))),
        COUNT("количество"),
        QUOTE(AnyOfTableColumn.of(
                TableColumnImpl.of("цена фьючерса"),
                TableColumnImpl.of("премия по опциону"))),
        VALUE("^сумма$"),
        VALUE_CURRENCY(OptionalTableColumn.of(
                TableColumnImpl.of("валюта суммы"))), // sometime does not exists
        MARKET_COMMISSION("комиссия тс"),
        BROKER_COMMISSION("комиссия брокера"),
        CLEARING_COMMISSION(OptionalTableColumn.of(
                TableColumnImpl.of("клиринговая комиссия"))); // only for Expiration table

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
