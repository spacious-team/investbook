/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.psb.foreignmarket;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.table_wrapper.api.*;
import ru.investbook.parser.AbstractReportTable;
import ru.investbook.parser.ForeignExchangeTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.foreignmarket.ForeignExchangeTransactionTable.FxTransactionTableHeader.*;

@Slf4j
public class ForeignExchangeTransactionTable extends AbstractReportTable<ForeignExchangeTransaction> {
    private static final String TABLE_NAME = "Информация о валютных сделках";

    public ForeignExchangeTransactionTable(PsbBrokerForeignMarketReport report) {
        super(report, TABLE_NAME, "", FxTransactionTableHeader.class);
    }

    @Override
    protected Collection<ForeignExchangeTransaction> getRow(Table table, TableRow row) {
        if (!table.getStringCellValue(row, POSITION_SWAP).trim().equalsIgnoreCase("нет")) {
            return emptyList();
        }
        Instant transactionInstant = convertToInstant(table.getStringCellValue(row, DATE_TIME));
        LocalDate transactionDate = LocalDate.ofInstant(transactionInstant, getReport().getReportZoneId());
        LocalDate reportDate = LocalDate.ofInstant(getReport().getReportEndDateTime(), getReport().getReportZoneId());
        if (!reportDate.equals(transactionDate)) {
            return emptyList();
        }
        String notUniqTransactionId = table.getStringCellValue(row, TRANSACTION);
        String orderDateTime = table.getStringCellValue(row, ORDER_DATE_TIME); // 08.02.2019 23:37
        long transactionId = Long.parseLong(notUniqTransactionId + orderDateTime.replaceAll("[.\\s:]", ""));
        boolean isBuy = table.getStringCellValue(row, DIRECTION).trim().equalsIgnoreCase("К");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        if (isBuy) {
            value = value.negate();
        }
        String contract = table.getStringCellValue(row, CONTRACT);
        String quoteCurrency = contract.substring(3, 6).toUpperCase(); // extracts RUB from USDRUB_TOM
        return Collections.singletonList(ForeignExchangeTransaction.builder()
                .timestamp(transactionInstant)
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .instrument(contract)
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .commission(table.getCurrencyCellValue(row, MARKET_COMMISSION).negate())
                .valueCurrency(quoteCurrency)
                .commissionCurrency("RUB")
                .build());
    }

    enum FxTransactionTableHeader implements TableColumnDescription {
        TRANSACTION("номер сделки"),
        ORDER_DATE_TIME("дата", "заключения сделки"),
        DATE_TIME("дата", "исполнения"),
        CONTRACT("инструмент"),
        DIRECTION("направление", "сделки"),
        COUNT("объем","в валюте лота"),
        VALUE("объем", "в сопряженной валюте"),
        MARKET_COMMISSION("комиссия", "биржи", "руб"),
        POSITION_SWAP("перенос", "позиции");

        @Getter
        private final TableColumn column;
        FxTransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
