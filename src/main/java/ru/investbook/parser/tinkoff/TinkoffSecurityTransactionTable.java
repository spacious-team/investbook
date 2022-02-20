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
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.Assert;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTable.TransactionTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.getSecurityId;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.getSecurityType;

@Slf4j
public class TinkoffSecurityTransactionTable extends SingleAbstractReportTable<AbstractTransaction> {

    private final SecurityCodeAndIsinTable codeAndIsin;

    public TinkoffSecurityTransactionTable(TinkoffBrokerReport report, SecurityCodeAndIsinTable codeAndIsin) {
        super(report,
                (cell) -> cell.startsWith("1.1 Информация о совершенных и исполненных сделках"),
                (cell) -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                TransactionTableHeader.class);
        this.codeAndIsin = codeAndIsin;
    }

    @Override
    protected AbstractTransaction parseRow(TableRow row) {
        long tradeId = row.getLongCellValueOrDefault(TRADE_ID, -1);
        if (tradeId == -1) return null;

        int securityId = getSecurityId(row, codeAndIsin, getReport().getSecurityRegistrar());
        boolean isBuy = row.getStringCellValue(OPERATION).toLowerCase().contains("покупка");
        int count = row.getIntCellValue(COUNT);
        BigDecimal amount = row.getBigDecimalCellValue(AMOUNT).abs();

        AbstractTransaction.AbstractTransactionBuilder<?, ?> builder = switch (getSecurityType(row)) {
            case STOCK -> SecurityTransaction.builder()
                    .timestamp(getStockAndBondTransactionInstant(row));
            case BOND, STOCK_OR_BOND -> {
                BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST).abs();
                yield SecurityTransaction.builder()
                        .timestamp(getStockAndBondTransactionInstant(row))
                        .accruedInterest(isBuy ? accruedInterest.negate() : accruedInterest);
            }
            case DERIVATIVE -> {
                BigDecimal valueInPoints = row.getBigDecimalCellValue(PRICE).abs()
                        .multiply(BigDecimal.valueOf(count));
                yield DerivativeTransaction.builder()
                        .timestamp(getDerivativeAndCurrencyPairTransactionInstant(row))
                        .valueInPoints(isBuy ? valueInPoints.negate() : valueInPoints);
            }
            case CURRENCY_PAIR -> ForeignExchangeTransaction.builder()
                    .timestamp(getDerivativeAndCurrencyPairTransactionInstant(row));
            case ASSET -> throw new IllegalArgumentException("Произвольный актив не поддерживается");
        };

        return builder
                .tradeId(String.valueOf(tradeId))
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count(count)
                .value(isBuy ? amount.negate() : amount)
                .valueCurrency(row.getStringCellValue(CURRENCY))
                .commission(getFee(row)) // converted to BROKER_FEE_CURRENCY
                .commissionCurrency(row.getStringCellValue(BROKER_FEE_CURRENCY))
                .build();
    }

    private Instant getStockAndBondTransactionInstant(TableRow row) {
        return getReport().convertToInstant(row.getStringCellValue(SETTLEMENT_DATE));
    }

    private Instant getDerivativeAndCurrencyPairTransactionInstant(TableRow row) {
        String dateTime = row.getStringCellValue(SETTLEMENT_DATE) + " " + row.getStringCellValue(TIME);
        return getReport().convertToInstant(dateTime);
    }

    private BigDecimal getFee(TableRow row) {
        BigDecimal fee = BigDecimal.ZERO;
        String feeCurrency = row.getStringCellValue(BROKER_FEE_CURRENCY);
        fee = addToFee(row, fee, feeCurrency, BROKER_FEE, BROKER_FEE_CURRENCY);
        fee = addToFee(row, fee, feeCurrency, MARKET_FEE, MARKET_FEE_CURRENCY);
        fee = addToFee(row, fee, feeCurrency, CLEARING_FEE, CLEARING_FEE_CURRENCY);
        return fee;
    }

    private BigDecimal addToFee(TableRow row, BigDecimal totalFee, String feeCurrency,
                                TransactionTableHeader feeComponent, TransactionTableHeader feeComponentCurrency) {
        BigDecimal feeAddition = row.getBigDecimalCellValueOrDefault(feeComponent, BigDecimal.ZERO).abs();
        if (Math.abs(feeAddition.floatValue()) > 1e-3) {
            String feeAdditionCurrency = row.getStringCellValue(feeComponentCurrency);
            // TODO convert feeAddition to feeCurrency
            Assert.isTrue(Objects.equals(feeCurrency, feeAdditionCurrency),
                    "Валюты комиссии брокера, биржи и клирингового центра различаются, не могу их сложить");
            totalFee = totalFee.add(feeAddition);
        }
        return totalFee;
    }

    protected enum TransactionTableHeader implements TableColumnDescription {
        TRADE_ID("номер", "сделки"),
        TIME("время"),
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
        MARKET_FEE("комиссия", "биржи"),
        MARKET_FEE_CURRENCY("валюта", "комиссии", "биржи"),
        CLEARING_FEE("комиссия", "клир.", "центра"),
        CLEARING_FEE_CURRENCY("валюта", "комиссии", "клир.", "центра"),
        SETTLEMENT_DATE("дата", "поставки");

        @Getter
        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
