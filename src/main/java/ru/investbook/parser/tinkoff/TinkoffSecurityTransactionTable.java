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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.TransactionValueAndFeeParser;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class TinkoffSecurityTransactionTable extends SingleAbstractReportTable<AbstractTransaction> {

    private final SecurityCodeAndIsinTable codeAndIsin;
    private final TransactionValueAndFeeParser transactionValueAndFeeParser;
    private final TinkoffSecurityTransactionTableHelper transactionTableHelper;

    public static ReportTable<AbstractTransaction> of(TinkoffBrokerReport report,
                                                      SecurityCodeAndIsinTable codeAndIsin,
                                                      TransactionValueAndFeeParser transactionValueAndFeeParser,
                                                      TinkoffSecurityTransactionTableHelper transactionTableHelper) {
        return WrappingReportTable.of(
                new TinkoffSecurityTransactionTable(
                        "1.1 Информация о совершенных и исполненных сделках",
                        report, codeAndIsin, transactionValueAndFeeParser, transactionTableHelper),
                new TinkoffSecurityTransactionTable(
                        "1.2 Информация о неисполненных сделках на конец отчетного периода",
                        report, codeAndIsin, transactionValueAndFeeParser, transactionTableHelper));
    }

    private TinkoffSecurityTransactionTable(String tableNamePrefix,
                                            TinkoffBrokerReport report,
                                            SecurityCodeAndIsinTable codeAndIsin,
                                            TransactionValueAndFeeParser transactionValueAndFeeParser,
                                            TinkoffSecurityTransactionTableHelper transactionTableHelper) {
        super(report,
                cell -> cell.startsWith(tableNamePrefix),
                cell -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                TransactionTableHeader.class);
        this.codeAndIsin = codeAndIsin;
        this.transactionValueAndFeeParser = transactionValueAndFeeParser;
        this.transactionTableHelper = transactionTableHelper;
    }

    @Override
    protected @Nullable AbstractTransaction parseRow(TableRow row) {
        String tradeId = row.getStringCellValueOrDefault(TRADE_ID, "");
        if (!hasLength(tradeId)) return null;
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        boolean isBuy = operation.contains("покупка");
        boolean isRepo = operation.contains("репо");
        if (isRepo) {
            // Сделки переноса РЕПО "РЕПО 1 Покупка" и "РЕПО 2 Продажа" имеют одинаковый tradeId и timestamp.
            // Это синтетические сделки, но эти сделки имеют комиссии, поэтому их нужно иметь возможность сохранить в БД.
            // Требуется уникальный tradeId для обоих РЕПО сделок.
            String tradeIdSuffix = isBuy ? "repobuy" : "reposell";
            tradeId += tradeIdSuffix;
        }

        int securityId = transactionTableHelper.getSecurityId(row, codeAndIsin, getReport().getSecurityRegistrar());
        int count = Math.abs(row.getIntCellValue(COUNT));
        BigDecimal amount = row.getBigDecimalCellValue(AMOUNT).abs();
        amount = isBuy ? amount.negate() : amount;
        count = isBuy ? count : -count;

        Instant timestamp;
        SecurityType securityType = transactionTableHelper.getSecurityType(row);
        AbstractTransaction.AbstractTransactionBuilder<?, ?> builder = switch (securityType) {
            case STOCK -> SecurityTransaction.builder()
                    .timestamp(timestamp = getStockAndBondTransactionInstant(row));
            case BOND, STOCK_OR_BOND -> {
                BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST).abs();
                yield SecurityTransaction.builder()
                        .timestamp(timestamp = getStockAndBondTransactionInstant(row))
                        .accruedInterest(isBuy ? accruedInterest.negate() : accruedInterest);
            }
            case DERIVATIVE -> {
                BigDecimal valueInPoints = row.getBigDecimalCellValue(PRICE)
                        .multiply(BigDecimal.valueOf(count))
                        .abs();
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
                        .stampDutyColumn(STAMP_DUTY)
                        .stampDutyCurrencyColumn(STAMP_DUTY_CURRENCY)
                        .build());

        return builder
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count(count)
                .value(valueAndFee.value())
                .valueCurrency(valueAndFee.valueCurrency())
                .fee(valueAndFee.fee().negate())
                .feeCurrency(valueAndFee.feeCurrency())
                .build();
    }

    private Instant getStockAndBondTransactionInstant(TableRow row) {
        String settlementDate = row.getStringCellValue(SETTLEMENT_DATE);
        // В старых отчетах одна дата, с января 2023 две: план/факт
        String[] dates = settlementDate.split("/");
        String date = (dates.length == 1) ? dates[0] : dates[1];
        return getReport().convertToInstant(date);
    }

    private Instant getDerivativeAndCurrencyPairTransactionInstant(TableRow row) {
        String dateTime = row.getStringCellValue(TRANSACTION_DATE) + " " + row.getStringCellValue(TRANSACTION_TIME);
        return getReport().convertToInstant(dateTime);
    }

    @RequiredArgsConstructor
    protected enum TransactionTableHeader implements TableHeaderColumn {
        TRADE_ID("номер", "сделки"),
        TRANSACTION_DATE("дата", "заклю", "чения"),
        TRANSACTION_TIME("время"),
        TYPE(optional("режим", "торгов")),
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
        MARKET_FEE(optional("комиссия", "биржи")),
        MARKET_FEE_CURRENCY(optional("валюта", "комиссии", "биржи")),
        CLEARING_FEE(optional("комиссия", "клир.", "центра")),
        CLEARING_FEE_CURRENCY(optional("валюта", "комиссии", "клир.", "центра")),
        STAMP_DUTY(optional("гербовый", "сбор")),
        STAMP_DUTY_CURRENCY(optional("валюта", "гербового", "сбора")),
        SETTLEMENT_DATE("дата", "расчетов");

        @Getter
        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }

        static OptionalTableColumn optional(String... words) {
            return OptionalTableColumn.of(PatternTableColumn.of(words));
        }
    }
}
