/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.TransactionValueAndFeeParser;

import java.math.BigDecimal;
import java.time.Instant;

import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTable.TransactionTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.getSecurityId;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.getSecurityType;

@Slf4j
public class TinkoffSecurityTransactionTable extends SingleAbstractReportTable<AbstractTransaction> {

    private final SecurityCodeAndIsinTable codeAndIsin;
    private final TransactionValueAndFeeParser transactionValueAndFeeParser;

    public TinkoffSecurityTransactionTable(TinkoffBrokerReport report,
                                           SecurityCodeAndIsinTable codeAndIsin,
                                           TransactionValueAndFeeParser transactionValueAndFeeParser) {
        super(report,
                (cell) -> cell.startsWith("1.1 Информация о совершенных и исполненных сделках"),
                (cell) -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                TransactionTableHeader.class);
        this.codeAndIsin = codeAndIsin;
        this.transactionValueAndFeeParser = transactionValueAndFeeParser;
    }

    @Override
    protected AbstractTransaction parseRow(TableRow row) {
        long _tradeId = row.getLongCellValueOrDefault(TRADE_ID, -1);
        if (_tradeId == -1) return null;
        String tradeId = String.valueOf(_tradeId);

        int securityId = getSecurityId(row, codeAndIsin, getReport().getSecurityRegistrar());
        boolean isBuy = row.getStringCellValue(OPERATION).toLowerCase().contains("покупка");
        int count = row.getIntCellValue(COUNT);
        BigDecimal amount = row.getBigDecimalCellValue(AMOUNT).abs();
        amount = isBuy ? amount.negate() : amount;
        count = isBuy ? count : -count;

        Instant timestamp = null;
        AbstractTransaction.AbstractTransactionBuilder<?, ?> builder = switch (getSecurityType(row)) {
            case STOCK -> SecurityTransaction.builder()
                    .timestamp(timestamp = getStockAndBondTransactionInstant(row));
            case BOND, STOCK_OR_BOND -> {
                BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST).abs();
                yield SecurityTransaction.builder()
                        .timestamp(timestamp = getStockAndBondTransactionInstant(row))
                        .accruedInterest(isBuy ? accruedInterest.negate() : accruedInterest);
            }
            case DERIVATIVE -> {
                BigDecimal valueInPoints = row.getBigDecimalCellValue(PRICE).abs()
                        .multiply(BigDecimal.valueOf(count));
                yield DerivativeTransaction.builder()
                        .timestamp(timestamp = getDerivativeAndCurrencyPairTransactionInstant(row))
                        .valueInPoints(isBuy ? valueInPoints.negate() : valueInPoints);
            }
            case CURRENCY_PAIR -> ForeignExchangeTransaction.builder()
                    .timestamp(timestamp = getDerivativeAndCurrencyPairTransactionInstant(row));
            case ASSET -> throw new IllegalArgumentException("Произвольный актив не поддерживается");
        };

        TransactionValueAndFeeParser.Result valueAndFee = transactionValueAndFeeParser.parse(
                transactionValueAndFeeParser.argumentsBuilder()
                        .row(row)
                        .portfolio(getReport().getPortfolio())
                        .tradeId(tradeId)
                        .transactionInstant(timestamp)
                        .value(amount)
                        .valueCurrencyColumn(CURRENCY)
                        .brokerFeeColumn(BROKER_FEE)
                        .brokerFeeCurrencyColumn(BROKER_FEE_CURRENCY)
                        .marketFeeColumn(MARKET_FEE)
                        .marketFeeCurrencyColumn(MARKET_FEE_CURRENCY)
                        .clearingFeeColumn(CLEARING_FEE)
                        .clearingFeeCurrencyColumn(CLEARING_FEE_CURRENCY)
                        .build());

        return builder
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count(count)
                .value(valueAndFee.value())
                .valueCurrency(valueAndFee.valueCurrency())
                .commission(valueAndFee.fee().negate())
                .commissionCurrency(valueAndFee.feeCurrency())
                .build();
    }

    private Instant getStockAndBondTransactionInstant(TableRow row) {
        return getReport().convertToInstant(row.getStringCellValue(SETTLEMENT_DATE));
    }

    private Instant getDerivativeAndCurrencyPairTransactionInstant(TableRow row) {
        String dateTime = row.getStringCellValue(TRANSACTION_DATE) + " " + row.getStringCellValue(TRANSACTION_TIME);
        return getReport().convertToInstant(dateTime);
    }

    @RequiredArgsConstructor
    protected enum TransactionTableHeader implements TableColumnDescription {
        TRADE_ID("номер", "сделки"),
        TRANSACTION_DATE("дата", "заклю", "чения"),
        TRANSACTION_TIME("время"),
        TYPE("режим", "торгов"),
        OPERATION("вид", "сделки"),
        SHORT_NAME("сокращен", "наименова"),
        CODE("код", "актива"),
        PRICE("Цена", "за едини"),
        COUNT("количество"),
        AMOUNT("сумма", "без", "нкд"),
        ACCRUED_INTEREST("^нкд$"), // суммарно по всем бумагам
        CURRENCY("валюта", "расчетов"),
        BROKER_FEE("комис", "брокера"),
        BROKER_FEE_CURRENCY("валю", "комис"),
        MARKET_FEE(OptionalTableColumn.of(
                TableColumnImpl.of("комиссия", "биржи"))),
        MARKET_FEE_CURRENCY(OptionalTableColumn.of(
                TableColumnImpl.of("валюта", "комиссии", "биржи"))),
        CLEARING_FEE(OptionalTableColumn.of(
                TableColumnImpl.of("комиссия", "клир.", "центра"))),
        CLEARING_FEE_CURRENCY(OptionalTableColumn.of(
                TableColumnImpl.of("валюта", "комиссии", "клир.", "центра"))),
        SETTLEMENT_DATE("дата", "расчетов");

        @Getter
        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
