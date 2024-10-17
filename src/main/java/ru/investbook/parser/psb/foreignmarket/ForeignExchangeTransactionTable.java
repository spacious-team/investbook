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

package ru.investbook.parser.psb.foreignmarket;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.time.Instant;

import static ru.investbook.parser.psb.foreignmarket.ForeignExchangeTransactionTable.FxTransactionTableHeader.*;

@Slf4j
public class ForeignExchangeTransactionTable extends SingleAbstractReportTable<AbstractTransaction> {
    private static final String TABLE_NAME = "Информация о валютных сделках";

    public ForeignExchangeTransactionTable(PsbBrokerForeignMarketReport report) {
        super(report, TABLE_NAME, "", FxTransactionTableHeader.class);
    }

    @Override
    protected ForeignExchangeTransaction parseRow(TableRow row) {
        String dateTime = row.getStringCellValue(DATE_TIME); // 08.02.2019 23:37
        Instant transactionInstant = convertToInstant(dateTime);
        String notUniqTradeId = row.getStringCellValue(TRADE_ID);
        String tradeId = notUniqTradeId + dateTime.replaceAll("[.\\s:]", "");
        boolean isBuy = row.getStringCellValue(DIRECTION).trim().equalsIgnoreCase("К");
        BigDecimal value = row.getBigDecimalCellValue(VALUE);
        if (isBuy) {
            value = value.negate();
        }
        String contract = row.getStringCellValue(CONTRACT);
        int securityId = getReport().getSecurityRegistrar().declareCurrencyPair(contract);
        String quoteCurrency = contract.substring(3, 6).toUpperCase(); // extracts RUB from USDRUB_TOM
        return ForeignExchangeTransaction.builder()
                .timestamp(transactionInstant)
                .tradeId(tradeId)
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .count((isBuy ? 1 : -1) * row.getIntCellValue(COUNT))
                .value(value)
                .fee(row.getBigDecimalCellValue(MARKET_COMMISSION).negate())
                .valueCurrency(quoteCurrency)
                .feeCurrency("RUB")
                .build();
    }

    @Getter
    enum FxTransactionTableHeader implements TableHeaderColumn {
        TRADE_ID("номер сделки"),
        DATE_TIME("дата", "заключения сделки"), // учет по дате сделки, а не дате исполнения, чтобы учесть неисполненные сделки
        CONTRACT("инструмент"),
        DIRECTION("направление", "сделки"),
        COUNT("объем", "в валюте лота"),
        VALUE("объем", "в сопряженной валюте"),
        MARKET_COMMISSION("комиссия", "биржи", "руб"),
        POSITION_SWAP("перенос", "позиции");

        private final TableColumn column;

        FxTransactionTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
