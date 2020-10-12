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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.investbook.parser.*;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.parser.table.excel.ExcelTable;
import ru.investbook.parser.uralsib.UralsibBrokerReport;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbSecurityTransactionTable.VtbSecurityTransactionTableHeader.*;

public class VtbSecurityTransactionTable extends AbstractReportTable<SecurityTransaction> {

    private static final String TABLE_NAME = "Завершенные в отчетном периоде сделки с ценными бумагами (обязательства прекращены)";
    private final BigDecimal minValue = BigDecimal.valueOf(0.01);

    protected VtbSecurityTransactionTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbSecurityTransactionTableHeader.class);
    }

    @Override
    protected Collection<SecurityTransaction> getRow(Table table, TableRow row) {
        String isin = table.getStringCellValue(row, NAME_AND_ISIN).split(",")[2].trim();
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE_WITH_ACCRUED_INTEREST);
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (accruedInterest.abs().compareTo(minValue) >= 0) {
            value = value.subtract(accruedInterest);
        } else {
            accruedInterest = BigDecimal.ZERO;
        }
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, BROKER_CLEARING_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_TRANSACTION_COMMISSION))
                .negate();
        String currency = UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, VALUE_CURRENCY));
        return Collections.singleton(SecurityTransaction.builder()
                .timestamp(((ExcelTable) table).getDateCellValue(row, DATE).toInstant())
                .transactionId(getTransactionId(table.getStringCellValue(row, TRANSACTION)))
                .portfolio(getReport().getPortfolio())
                .isin(isin)
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(commission)
                .valueCurrency(currency)
                .commissionCurrency(currency)
                .build());
    }

    private static Long getTransactionId(String id) {
        long numbericId = 0;
        for (char symbol : id.toCharArray()) {
            numbericId *= 10;
            if (Character.isDigit(symbol)) {
                numbericId += Character.getNumericValue(symbol);
            } else {
                numbericId += symbol;
            }
        }
        return  numbericId;
    }

    @RequiredArgsConstructor
    enum VtbSecurityTransactionTableHeader implements TableColumnDescription {
        DATE("плановая дата поставки"),
        TRANSACTION("№ сделки"),
        NAME_AND_ISIN("наименование", "isin"),
        DIRECTION("вид сделки"),
        COUNT("количество"),
        VALUE_WITH_ACCRUED_INTEREST("сумма сделки в валюте расчетов", "с учетом НКД"),
        ACCRUED_INTEREST("НКД", "по сделке в валюте расчетов"),
        VALUE_CURRENCY("Валюта расчетов"),
        BROKER_CLEARING_COMMISSION("Комиссия Банка за расчет по сделке"),
        BROKER_TRANSACTION_COMMISSION("Комиссия Банка за заключение сделки");

        @Getter
        private final TableColumn column;

        VtbSecurityTransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
