/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.math.BigDecimal;

import static ru.investbook.parser.vtb.VtbForeignExchangeTransactionTable.FxTransactionTableHeader.*;

public class VtbForeignExchangeTransactionTable extends SingleAbstractReportTable<ForeignExchangeTransaction> {

    // Не использовать "Завершенные в отчетном периоде сделки с иностранной валютой (обязательства прекращены)",
    // иначе не примутся в расчет сделки выполненные без обналичивания валюты
    private static final String TABLE_NAME = "Заключенные в отчетном периоде сделки с иностранной валютой";

    protected VtbForeignExchangeTransactionTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, null, FxTransactionTableHeader.class);
    }

    @Override
    protected ForeignExchangeTransaction parseRow(TableRow row) {
        boolean isBuy = row.getStringCellValue(DIRECTION).trim().equalsIgnoreCase("Покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isBuy) {
            value = value.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .negate();
        return ForeignExchangeTransaction.builder()
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .transactionId(row.getStringCellValue(TRANSACTION))
                .portfolio(getReport().getPortfolio())
                .security(row.getStringCellValue(INSTRUMENT))
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .commission(commission)
                .valueCurrency(VtbBrokerReport.convertToCurrency(row.getStringCellValue(VALUE_CURRENCY)))
                .commissionCurrency("RUB")
                .build();
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
