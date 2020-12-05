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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.MultiLineTableColumn;
import org.spacious_team.table_wrapper.api.RelativePositionTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.SecurityTransactionTable.TransactionTableHeader.*;

@Slf4j
public class SecurityTransactionTable extends AbstractReportTable<SecurityTransaction> {
    private static final String TABLE_NAME = "Биржевые сделки с ценными бумагами в отчетном периоде";
    private final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final PortfolioPropertyTable portfolioPropertyTable;

    public SecurityTransactionTable(UralsibBrokerReport report,
                                    PortfolioPropertyTable portfolioPropertyTable) {
        super(report, TABLE_NAME, "", TransactionTableHeader.class, 2);
        this.portfolioPropertyTable = portfolioPropertyTable;
    }

    @Override
    protected Collection<SecurityTransaction> getRow(Table table, TableRow row) {
        String transactionId = getTransactionId(table, row, TRANSACTION);
        if (transactionId == null) return emptyList();

        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        String valueCurrency = getCurrency(table, row, VALUE_CURRENCY, value, "RUB");
        BigDecimal marketCommission = table.getCurrencyCellValue(row, MARKET_COMMISSION);
        String marketCommissionCurrency = getCurrency(table, row, MARKET_COMMISSION_CURRENCY, marketCommission, valueCurrency);
        BigDecimal brokerCommission = table.getCurrencyCellValue(row, BROKER_COMMISSION);
        String brokerCommissionCurrency = getCurrency(table, row, BROKER_COMMISSION_CURRENCY, brokerCommission, valueCurrency);
        Instant timestamp = getReport().convertToInstant(table.getStringCellValue(row, DATE_TIME));
        BigDecimal commission = getConvertedCommission(marketCommission, marketCommissionCurrency, valueCurrency, timestamp)
                .add(getConvertedCommission(brokerCommission, brokerCommissionCurrency, valueCurrency, timestamp))
                .negate();

        return Collections.singletonList(SecurityTransaction.builder()
                .timestamp(timestamp)
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .security(table.getStringCellValue(row, ISIN))
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest((accruedInterest.abs().compareTo(minValue) >= 0) ? accruedInterest : BigDecimal.ZERO)
                .commission(commission)
                .valueCurrency(valueCurrency)
                .commissionCurrency(valueCurrency)
                .build());
    }

    static String getTransactionId(Table table, TableRow row, TableColumnDescription column) {
        try {
            // some numbers (doubles) represented by string type cells
            return String.valueOf(table.getLongCellValue(row, column));
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrency(Table table, TableRow row, TableColumnDescription currencyColumn,
                               BigDecimal commission, String defaultCurrency) {
        return Optional.ofNullable(
                table.getStringCellValueOrDefault(row, currencyColumn, null))
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
        if (commission.compareTo(minValue) < 0) {
            // is commission == 0 report contains commissionCurrency = ""
            return commission;
        } else if (commissionCurrency.isEmpty() || targetCurrency.isEmpty()) {
            log.warn("Не указана валюта комиссии для комиссии {} в файле {}", commission, getReport().getPath().getFileName());
            return commission;
        }
        BigDecimal exchangeRate = portfolioPropertyTable.getExchangeRate(commissionCurrency,
                targetCurrency, timestamp);
        return commission.multiply(exchangeRate);
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
        TransactionTableHeader(String ... words) {
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
