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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.portfolio.portfolio.parser.uralsib.SecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class SecurityTransactionTable extends AbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = "Биржевые сделки с ценными бумагами в отчетном периоде";
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public SecurityTransactionTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", TransactionTableHeader.class, 2);
    }

    @Override
    protected Collection<SecurityTransaction> getRow(ExcelTable table, Row row) {
        long transactionId;
        if (table.getCell(row, TRANSACTION).getCellType() == CellType.STRING) {
            try {
                // some numbers represented by string type cells
                transactionId = Long.parseLong(table.getStringCellValue(row, TRANSACTION));
            } catch (NumberFormatException e) {
                return emptyList();
            }
        } else if (table.getCell(row, TRANSACTION).getCellType() == CellType.NUMERIC) {
            transactionId = table.getLongCellValue(row, TRANSACTION);
        } else {
            return emptyList();
        }

        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal marketCommission = table.getCurrencyCellValue(row, MARKET_COMMISSION);
        String marketCommissionCurrency = table.getStringCellValue(row, MARKET_COMMISSION_CURRENCY);
        BigDecimal brokerCommission = table.getCurrencyCellValue(row, BROKER_COMMISSION);
        String brokerCommissionCurrency = table.getStringCellValue(row, BROKER_COMMISSION_CURRENCY);
        BigDecimal commission = BigDecimal.ZERO;
        String commissionCurrency = null;
        if (marketCommission.abs().compareTo(minValue) >= 0) {
            commission = marketCommission;
            commissionCurrency = marketCommissionCurrency;
        }
        if (brokerCommission.abs().compareTo(minValue) >= 0) {
            if (commissionCurrency == null) {
                commissionCurrency = brokerCommissionCurrency;
            } else if (!commissionCurrency.equalsIgnoreCase(brokerCommissionCurrency)) {
                throw new IllegalArgumentException("Для транзакции " + transactionId +
                        " не могу сложить коммиссию в валютах " + commissionCurrency + " и " + brokerCommissionCurrency);
            }
            commission = commission.add(brokerCommission);
        }
        commission = commission.negate();

        return Collections.singletonList(SecurityTransaction.builder()
                .timestamp(getReport().convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .isin(table.getStringCellValue(row, ISIN))
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest((accruedInterest.abs().compareTo(minValue) >= 0) ? accruedInterest : BigDecimal.ZERO)
                .commission(commission)
                .valueCurrency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, VALUE_CURRENCY)))
                .commissionCurrency(UralsibBrokerReport.convertToCurrency(commissionCurrency))
                .build());
    }

    enum TransactionTableHeader implements TableColumnDescription {
        DATE_TIME("дата", "поставки"),
        TRANSACTION("номер сделки"),
        ISIN("isin"),
        DIRECTION("вид", "сделки"),
        COUNT("количество", "цб"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта суммы"),
        ACCRUED_INTEREST("нкд"),
        MARKET_COMMISSION(16),
        MARKET_COMMISSION_CURRENCY(17),
        BROKER_COMMISSION(20),
        BROKER_COMMISSION_CURRENCY(21);

        @Getter
        private final TableColumn column;
        TransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }

        TransactionTableHeader(int columnIndex) {
            this.column = ConstantPositionTableColumn.of(columnIndex);
        }
    }
}
