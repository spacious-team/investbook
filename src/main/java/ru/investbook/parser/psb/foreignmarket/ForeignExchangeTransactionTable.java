/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.psb.foreignmarket;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.psb.foreignmarket.ForeignExchangeTransactionTable.FxTransactionTableHeader.*;

@Slf4j
public class ForeignExchangeTransactionTable extends AbstractReportTable<ForeignExchangeTransaction> {
    private static final String TABLE_NAME = "Информация о валютных сделках";

    public ForeignExchangeTransactionTable(PsbBrokerForeignMarketReport report) {
        super(report, TABLE_NAME, "", FxTransactionTableHeader.class);
    }

    @Override
    protected Collection<ForeignExchangeTransaction> getRow(TableRow row) {
        String dateTime = row.getStringCellValue(DATE_TIME); // 08.02.2019 23:37
        Instant transactionInstant = convertToInstant(dateTime);
        String notUniqTransactionId = row.getStringCellValue(TRANSACTION);
        String transactionId = notUniqTransactionId + dateTime.replaceAll("[.\\s:]", "");
        boolean isBuy = row.getStringCellValue(DIRECTION).trim().equalsIgnoreCase("К");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isBuy) {
            value = value.negate();
        }
        String contract = row.getStringCellValue(CONTRACT);
        String quoteCurrency = contract.substring(3, 6).toUpperCase(); // extracts RUB from USDRUB_TOM
        return Collections.singletonList(ForeignExchangeTransaction.builder()
                .timestamp(transactionInstant)
                .transactionId(transactionId)
                .portfolio(getReport().getPortfolio())
                .security(contract)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .commission(row.getBigDecimalCellValue(MARKET_COMMISSION).negate())
                .valueCurrency(quoteCurrency)
                .commissionCurrency("RUB")
                .build());
    }

    enum FxTransactionTableHeader implements TableColumnDescription {
        TRANSACTION("номер сделки"),
        DATE_TIME("дата", "заключения сделки"), // учет по дате сделки, а не дате исполнения, чтобы учесть неисполненные сделки
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
