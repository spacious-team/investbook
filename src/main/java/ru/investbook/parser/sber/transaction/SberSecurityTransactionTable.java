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

package ru.investbook.parser.sber.transaction;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;

import static ru.investbook.parser.sber.transaction.SberSecurityTransactionTable.SberTransactionTableHeader.*;

@Slf4j
public class SberSecurityTransactionTable extends AbstractReportTable<SecurityTransaction> {
    private static final String FIRST_LINE = "Номер договора";

    protected SberSecurityTransactionTable(BrokerReport report) {
        super(report, "Сделки", FIRST_LINE, null, SberTransactionTableHeader.class);
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        boolean isBuy = "Покупка".equalsIgnoreCase(row.getStringCellValue(OPERATION));
        boolean isBond = "Облигация".equalsIgnoreCase(row.getStringCellValueOrDefault(SECURITY_TYPE, null));
        BigDecimal value = row.getBigDecimalCellValue(VALUE).abs();
        BigDecimal accruedInterest = isBond ? row.getBigDecimalCellValue(ACCRUED_INTEREST).abs() : BigDecimal.ZERO;
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        String currency = row.getStringCellValue(CURRENCY);
        return SecurityTransaction.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .transactionId(String.valueOf(row.getLongCellValue(TRANSACTION))) // may be double numbers in future
                .security(getSecurity(row))
                .count(row.getIntCellValue(COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(row.getBigDecimalCellValue(MARKET_COMMISSION)
                        .add(row.getBigDecimalCellValue(BROKER_COMMISSION)))
                .valueCurrency(currency)
                .commissionCurrency(currency)
                .build();
    }

    private static String getSecurity(TableRow row) {
        String nameAndIsin = row.getStringCellValue(NAME_AND_ISIN); // format: "<name>\s*(<isin>)"
        int start = nameAndIsin.indexOf('(') + 1;
        int end = nameAndIsin.indexOf(')');
        return nameAndIsin.substring(start, (end == -1) ? nameAndIsin.length() : end);
    }

    enum SberTransactionTableHeader implements TableColumnDescription {
        PORTFOLIO("Номер договора"),
        TRANSACTION("Номер сделки"),
        DATE_TIME("Дата расчётов"),
        NAME_AND_ISIN("Код финансового инструмента"),
        SECURITY_TYPE("Тип финансового инструмента"),
        OPERATION("Операция"),
        COUNT("Количество"),
        VALUE("Объём сделки"), // без учета НКД и комиссий
        ACCRUED_INTEREST("НКД"), // за все бумаги
        MARKET_COMMISSION("Комиссия торговой системы"),
        BROKER_COMMISSION("Комиссия банка"),
        CURRENCY("Валюта"),
        EXCHANGE_RATE("Курс"); // обменный курс валюты? "1" для CURRENCY = RUB

        @Getter
        private final TableColumn column;
        SberTransactionTableHeader(String words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
