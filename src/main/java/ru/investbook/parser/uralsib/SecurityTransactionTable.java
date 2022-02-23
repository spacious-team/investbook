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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.RelativePositionTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static ru.investbook.parser.uralsib.SecurityRegistryHelper.declareStockOrBond;
import static ru.investbook.parser.uralsib.SecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class SecurityTransactionTable extends SingleAbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = "Биржевые сделки с ценными бумагами в отчетном периоде";
    private final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final ForeignExchangeRateTable foreignExchangeRateTable;

    public SecurityTransactionTable(UralsibBrokerReport report,
                                    ForeignExchangeRateTable foreignExchangeRateTable) {
        super(report, TABLE_NAME, "", TransactionTableHeader.class, 2);
        this.foreignExchangeRateTable = foreignExchangeRateTable;
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        String tradeId = getTradeId(row, TRADE_ID);
        if (tradeId == null) return null;

        boolean isBuy = row.getStringCellValue(DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        BigDecimal accruedInterest = row.getBigDecimalCellValue(ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        String valueCurrency = getCurrency(row, VALUE_CURRENCY, value, "RUB");
        BigDecimal marketCommission = row.getBigDecimalCellValue(MARKET_COMMISSION).abs();
        String marketCommissionCurrency = getCurrency(row, MARKET_COMMISSION_CURRENCY, marketCommission, valueCurrency);
        BigDecimal brokerCommission = row.getBigDecimalCellValue(BROKER_COMMISSION).abs();
        String brokerCommissionCurrency = getCurrency(row, BROKER_COMMISSION_CURRENCY, brokerCommission, valueCurrency);
        Instant timestamp = convertToInstant(row.getStringCellValue(DATE_TIME));

        BigDecimal commission;
        String commissionCurrency;
        if (marketCommissionCurrency.equals(brokerCommissionCurrency)) {
            commission = brokerCommission.add(marketCommission);
            commissionCurrency = brokerCommissionCurrency;
        } else if (marketCommission.compareTo(minValue) < 0 && brokerCommission.compareTo(minValue) < 0) {
            commission = BigDecimal.ZERO;
            commissionCurrency = brokerCommissionCurrency;
        } else if (marketCommission.compareTo(minValue) < 0) {
            commission = brokerCommission;
            commissionCurrency = brokerCommissionCurrency;
        } else if (brokerCommission.compareTo(minValue) < 0) {
            commission = marketCommission;
            commissionCurrency = marketCommissionCurrency;
        } else {
            try {
                commission = marketCommission
                        .add(getConvertedCommission(brokerCommission, brokerCommissionCurrency, marketCommissionCurrency, timestamp))
                        .negate();
                commissionCurrency = marketCommissionCurrency;
            } catch (Exception e) {
                String msg = "Не возможно просуммировать между собой комиссии ТС и брокера, валюты разные, обменный курс не известен";
                if (marketCommissionCurrency.equals(valueCurrency)) {
                    value = value.subtract(marketCommission);
                    commission = brokerCommission;
                    commissionCurrency = brokerCommissionCurrency;
                    log.warn("{}. Комиссия ТС включена в сумму сделки {}, т.к. они оба в одной валюте: {}",
                            msg, tradeId, valueCurrency);
                } else if (brokerCommissionCurrency.equals(valueCurrency)) {
                    value = value.subtract(brokerCommission);
                    commission = marketCommission;
                    commissionCurrency = marketCommissionCurrency;
                    log.warn("{}. Комиссия брокера включена в сумму сделки {}, т.к. они оба в одной валюте: {}",
                            msg, tradeId, valueCurrency);
                } else {
                    throw new RuntimeException(msg, e);
                }
            }
        }
        String isin = row.getStringCellValue(ISIN);
        int securityId = declareStockOrBond(isin, null, getReport().getSecurityRegistrar()); // TODO  set .name() also

        return SecurityTransaction.builder()
                .timestamp(timestamp)
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .accruedInterest((accruedInterest.abs().compareTo(minValue) >= 0) ? accruedInterest : BigDecimal.ZERO)
                .commission(commission.negate())
                .valueCurrency(valueCurrency)
                .commissionCurrency(commissionCurrency)
                .build();
    }

    static String getTradeId(TableRow row, TableColumnDescription column) {
        try {
            // some numbers (doubles) represented by string type cells
            return String.valueOf(row.getLongCellValue(column));
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrency(TableRow row, TableColumnDescription currencyColumn,
                               BigDecimal commission, String defaultCurrency) {
        return Optional.ofNullable(
                        row.getStringCellValueOrDefault(currencyColumn, null))
                .or(() -> (commission.abs().compareTo(minValue) < 0) ?
                        Optional.ofNullable(defaultCurrency) :
                        Optional.empty())
                .map(UralsibBrokerReport::convertToCurrency)
                .orElseThrow(() -> new RuntimeException("No currency provided in column " + currencyColumn));
    }

    /**
     * Returns commission converted from commissionCurrency to targetCurrency
     */
    private BigDecimal getConvertedCommission(BigDecimal commission, String commissionCurrency,
                                              String targetCurrency, Instant timestamp) {
        BigDecimal exchangeRate = foreignExchangeRateTable.getExchangeRate(commissionCurrency, targetCurrency, timestamp);
        return commission.multiply(exchangeRate);
    }

    enum TransactionTableHeader implements TableColumnDescription {
        DATE_TIME("дата", "поставки"),
        TRADE_ID("номер сделки"),
        ISIN("isin"),
        DIRECTION("вид", "сделки"),
        COUNT("количество", "цб"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта суммы"),
        ACCRUED_INTEREST("нкд"),
        MARKET_COMMISSION(
                TableColumnImpl.of("комиссия тс"),
                TableColumnImpl.of("всего")),
        MARKET_COMMISSION_CURRENCY(
                TableColumnImpl.of("комиссия тс"),
                TableColumnImpl.of("валюта списания")),
        BROKER_COMMISSION_CURRENCY(
                TableColumnImpl.of("комиссия брокера"),
                TableColumnImpl.of("валюта списания")),
        BROKER_COMMISSION(BROKER_COMMISSION_CURRENCY, -1);

        @Getter
        private final TableColumn column;

        TransactionTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }

        TransactionTableHeader(TableColumn... rowDescriptors) {
            this.column = MultiLineTableColumn.of(rowDescriptors);
        }

        TransactionTableHeader(TransactionTableHeader relatedColumn, int relatedOffset) {
            this.column = RelativePositionTableColumn.of(relatedColumn.getColumn(), relatedOffset);
        }
    }
}
