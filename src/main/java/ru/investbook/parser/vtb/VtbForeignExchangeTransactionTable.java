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
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.investbook.parser.vtb.VtbForeignExchangeTransactionTable.FxTransactionTableHeader.*;

public class VtbForeignExchangeTransactionTable extends SingleInitializableReportTable<ForeignExchangeTransaction> {

    // Не использовать "Завершенные в отчетном периоде сделки с иностранной валютой (обязательства прекращены)",
    // иначе не примутся в расчет сделки выполненные без обналичивания валюты
    private static final String[] TABLE_NAMES = {
            "Заключенные в отчетном периоде сделки с иностранной валютой",
            // Таблица незавершенных сделок может не содержать комиссию по сделкам после 19-00. Парсинг по таким сделкам
            // падает. Загружаем такие сделки из следующего отчета в таблице завершенных сделок.
           "Завершенные в отчетном периоде сделки с иностранной валютой (обязательства прекращены)"};

    protected VtbForeignExchangeTransactionTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<ForeignExchangeTransaction> parseTable() {
        return Stream.of(TABLE_NAMES)
                .map(this::parseTable)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Collection<ForeignExchangeTransaction> parseTable(String tableName) {
        return getReport().getReportPage()
                .create(tableName, FxTransactionTableHeader.class)
                .getData(getReport(), this::parseRow);
    }

    protected ForeignExchangeTransaction parseRow(TableRow row) {
        boolean isBuy = row.getStringCellValue(DIRECTION).trim().equalsIgnoreCase("Покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isBuy) {
            value = value.negate();
        }
        BigDecimal commission = row.getBigDecimalCellValue(MARKET_COMMISSION)
                .add(row.getBigDecimalCellValue(BROKER_COMMISSION))
                .negate();
        String instrument = row.getStringCellValue(INSTRUMENT);
        int securityId = getReport().getSecurityRegistrar().declareCurrencyPair(instrument);
        return ForeignExchangeTransaction.builder()
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .tradeId(row.getStringCellValue(TRADE_ID))
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .commission(commission)
                .valueCurrency(VtbBrokerReport.convertToCurrency(row.getStringCellValue(VALUE_CURRENCY)))
                .commissionCurrency("RUB")
                .build();
    }

    enum FxTransactionTableHeader implements TableColumnDescription {
        TRADE_ID("№ сделки"),
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

        FxTransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
