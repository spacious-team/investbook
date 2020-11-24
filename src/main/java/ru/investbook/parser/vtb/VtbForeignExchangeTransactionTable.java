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
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.table_wrapper.api.*;
import org.spacious_team.table_wrapper.excel.ExcelTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbForeignExchangeTransactionTable.FxTransactionTableHeader.*;

public class VtbForeignExchangeTransactionTable extends AbstractReportTable<ForeignExchangeTransaction> {

    private static final String TABLE_NAME = "Заключенные в отчетном периоде сделки с иностранной валютой";

    protected VtbForeignExchangeTransactionTable(BrokerReport report) {
        super(report, TABLE_NAME, null, FxTransactionTableHeader.class);
    }

    @Override
    protected Collection<ForeignExchangeTransaction> getRow(Table table, TableRow row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).trim().equalsIgnoreCase("Покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        if (isBuy) {
            value = value.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        return Collections.singletonList(ForeignExchangeTransaction.builder()
                .timestamp(((ExcelTable) table).getDateCellValue(row, DATE_TIME).toInstant())
                .transactionId(table.getStringCellValue(row, TRANSACTION))
                .portfolio(getReport().getPortfolio())
                .instrument(table.getStringCellValue(row, INSTRUMENT))
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .commission(commission)
                .valueCurrency(VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, VALUE_CURRENCY)))
                .commissionCurrency("RUB")
                .build());
    }

    enum FxTransactionTableHeader implements TableColumnDescription {
        TRANSACTION("№ сделки"),
        INSTRUMENT("Финансовый инструмент"),
        DATE_TIME("Дата и время заключения сделки"),
        DIRECTION("Вид сделки"),
        COUNT("Количество"),
        VALUE("Сумма сделки в валюте расчетов"),
        VALUE_CURRENCY("Валюта расчетов"),
        MARKET_COMMISSION("Комиссия", "за расчет по сделке"),
        BROKER_COMMISSION("Комиссия", "за заключение сделки");

        @Getter
        private final TableColumn column;
        FxTransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
