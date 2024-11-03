/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.AnyOfTableColumn;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.RelativePositionTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.TransactionValueAndFeeParser;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static ru.investbook.parser.uralsib.SecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class SecurityTransactionTable extends SingleAbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = "Биржевые сделки с ценными бумагами в отчетном периоде";
    private final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final SecuritiesTable securitiesTable;
    private final ForeignExchangeRateTable foreignExchangeRateTable;
    private final TransactionValueAndFeeParser transactionValueAndFeeParser;
    private @Nullable Security security = null;

    public SecurityTransactionTable(UralsibBrokerReport report,
                                    SecuritiesTable securitiesTable,
                                    ForeignExchangeRateTable foreignExchangeRateTable,
                                    TransactionValueAndFeeParser transactionValueAndFeeParser) {
        this(report, TABLE_NAME, securitiesTable, foreignExchangeRateTable, transactionValueAndFeeParser);
    }

    protected SecurityTransactionTable(UralsibBrokerReport report,
                                       String tableName,
                                       SecuritiesTable securitiesTable,
                                       ForeignExchangeRateTable foreignExchangeRateTable,
                                       TransactionValueAndFeeParser transactionValueAndFeeParser) {
        super(report, tableName, "", TransactionTableHeader.class, 2);
        this.securitiesTable = securitiesTable;
        this.foreignExchangeRateTable = foreignExchangeRateTable;
        this.transactionValueAndFeeParser = transactionValueAndFeeParser;
    }

    @Override
    protected @Nullable SecurityTransaction parseRow(TableRow row) {
        @Nullable String tradeId = getTradeId(row, TRADE_ID);
        if (tradeId == null) {
            security = getSecurity(row);
            return null;
        }

        requireNonNull(security, "Не известная ЦБ");
        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        Instant timestamp = convertToInstant(row.getStringCellValue(DATE_TIME));

        TransactionValueAndFeeParser.Result valueAndFee = transactionValueAndFeeParser.parse(
                transactionValueAndFeeParser.argumentsBuilder()
                        .row(row)
                        .portfolio(getReport().getPortfolio())
                        .tradeId(tradeId)
                        .value(value)
                        .valueCurrencyColumn(VALUE_CURRENCY)
                        .transactionInstant(timestamp)
                        .exchangeRateProvider(foreignExchangeRateTable::getExchangeRate)
                        .brokerFeeColumn(BROKER_COMMISSION)
                        .brokerFeeCurrencyColumn(BROKER_COMMISSION_CURRENCY)
                        .marketFeeColumn(MARKET_COMMISSION)
                        .marketFeeCurrencyColumn(MARKET_COMMISSION_CURRENCY)
                        .build());

        return SecurityTransaction.builder()
                .timestamp(timestamp)
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(requireNonNull(security.getId()))
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(valueAndFee.value())
                .accruedInterest((accruedInterest.abs().compareTo(minValue) >= 0) ? accruedInterest : BigDecimal.ZERO)
                .fee(valueAndFee.fee().negate())
                .valueCurrency(valueAndFee.valueCurrency())
                .feeCurrency(valueAndFee.feeCurrency())
                .build();
    }

    static @Nullable String getTradeId(TableRow row, TableHeaderColumn column) {
        try {
            // some numbers (doubles) represented by string type cells
            return String.valueOf(row.getLongCellValue(column));
        } catch (Exception e) {
            return null;
        }
    }

    private @Nullable Security getSecurity(TableRow row) {
        //noinspection DataFlowIssue
        return Optional.ofNullable(row.getStringCellValueOrDefault(TRADE_ID, null))
                .filter(securityDescription -> !securityDescription.startsWith("Итого"))
                .map(securityDescription -> securityDescription.split(" "))
                .map(parts -> parts[parts.length - 1])
                .map(securitiesTable::getSecurityByCfi)
                .orElse(null);
    }

    @Getter
    enum TransactionTableHeader implements TableHeaderColumn {
        DATE_TIME(
                AnyOfTableColumn.of(
                        MultiLineTableColumn.of(                                // таблица "Специальные сделки РЕПО для переноса длинной позиции"
                                PatternTableColumn.of("дата", "поставки"),
                                PatternTableColumn.of("плановая")),
                        PatternTableColumn.of("дата", "поставки"))),        // таблица "Биржевые сделки с ценными бумагами в отчетном периоде"
        TRADE_ID("номер сделки"),
        DIRECTION("вид", "сделки"),
        COUNT("количество", "цб"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта суммы"),
        ACCRUED_INTEREST("нкд"),
        MARKET_COMMISSION(
                AnyOfTableColumn.of(
                        MultiLineTableColumn.of(                               // old report
                                PatternTableColumn.of("комиссия тс"),
                                PatternTableColumn.of("всего")),
                        PatternTableColumn.of("комиссия тс", "всего"))),   // new report
        MARKET_COMMISSION_CURRENCY(
                AnyOfTableColumn.of(                                           // old report
                        MultiLineTableColumn.of(
                                PatternTableColumn.of("комиссия тс"),
                                PatternTableColumn.of("валюта списания")),
                        VALUE_CURRENCY.getColumn())),                          // new report (fallback to value currency)
        BROKER_COMMISSION(
                AnyOfTableColumn.of(
                        RelativePositionTableColumn.of(                        // old report
                                MultiLineTableColumn.of(
                                        PatternTableColumn.of("комиссия брокера"),
                                        PatternTableColumn.of("валюта списания")),
                                -1),
                        PatternTableColumn.of("комиссия брокера", "всего"))),  // new report
        BROKER_COMMISSION_CURRENCY(
                AnyOfTableColumn.of(                                           // old report
                        MultiLineTableColumn.of(
                                PatternTableColumn.of("комиссия брокера"),
                                PatternTableColumn.of("валюта списания")),
                        VALUE_CURRENCY.getColumn()));                          // new report (fallback to value currency)

        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }

        TransactionTableHeader(TableColumn column) {
            this.column = column;
        }
    }
}
