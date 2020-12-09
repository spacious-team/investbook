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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;
import static ru.investbook.parser.vtb.VtbSecurityTransactionTable.VtbSecurityTransactionTableHeader.*;

public class VtbSecurityTransactionTable extends AbstractReportTable<SecurityTransaction> {

    private static final String TABLE_NAME = "Завершенные в отчетном периоде сделки с ценными бумагами (обязательства прекращены)";

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
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        String currency = VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, VALUE_CURRENCY));
        return Collections.singleton(SecurityTransaction.builder()
                .timestamp(((ExcelTable) table).getDateCellValue(row, DATE).toInstant())
                .transactionId(table.getStringCellValue(row, TRANSACTION))
                .portfolio(getReport().getPortfolio())
                .security(isin)
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(commission)
                .valueCurrency(currency)
                .commissionCurrency(currency)
                .build());
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
        MARKET_COMMISSION("Комиссия Банка за расчет по сделке"),
        BROKER_COMMISSION("Комиссия Банка за заключение сделки");

        @Getter
        private final TableColumn column;

        VtbSecurityTransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
