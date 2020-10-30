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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.table_wrapper.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends AbstractReportTable<DerivativeTransaction> {
    private static final String TABLE_NAME = "СДЕЛКИ С ФЬЮЧЕРСАМИ И ОПЦИОНАМИ";
    private static final String TABLE_END_TEXT = "ДВИЖЕНИЕ ДЕНЕЖНЫХ СРЕДСТВ ЗА ОТЧЕТНЫЙ ПЕРИОД";

    public DerivativeTransactionTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, FortsTableHeader.class, 2);
    }

    @Override
    protected Collection<DerivativeTransaction> getRow(Table table, TableRow row) {
        Long transactionId = SecurityTransactionTable.getTransactionId(table, row, TRANSACTION);
        if (transactionId == null) return emptyList();

        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
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
                .negate();
        List<DerivativeTransaction> transactionInfo = new ArrayList<>(2);
        DerivativeTransaction.DerivativeTransactionBuilder builder = DerivativeTransaction.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .contract(table.getStringCellValue(row, CONTRACT))
                .count((isBuy ? 1 : -1) * count);
        transactionInfo.add(builder
                .value(value)
                .commission(commission)
                .valueCurrency(valueCurrency)
                .commissionCurrency("RUB") // FORTS, only RUB
                .build());
        transactionInfo.add(builder
                .value(valueInPoints)
                .commission(BigDecimal.ZERO)
                .valueCurrency(DerivativeTransaction.QUOTE_CURRENCY)
                .commissionCurrency("RUB") // FORTS, only RUB
                .build());
        return transactionInfo;
    }

    @RequiredArgsConstructor
    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата расчетов"),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("наименование контракта"),
        DIRECTION("вид сделки"),
        COUNT("количество контрактов"),
        QUOTE(AnyOfTableColumn.of(
                TableColumnImpl.of("цена фьючерса"),
                TableColumnImpl.of("премия по опциону"))),
        VALUE("^сумма$"),
        VALUE_CURRENCY(OptionalTableColumn.of(
                TableColumnImpl.of("валюта суммы"))), // sometime does not exists
        MARKET_COMMISSION("комиссия тс"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
