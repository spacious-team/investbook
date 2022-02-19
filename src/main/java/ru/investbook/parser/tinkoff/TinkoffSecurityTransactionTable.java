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
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.regex.Pattern;

@Slf4j
public class TinkoffSecurityTransactionTable extends AbstractReportTable<AbstractTransaction> {

    private static final Pattern lastRowPattern = Pattern.compile("[0-9]+\\.[0-9]+\\s+\\b");

    public TinkoffSecurityTransactionTable(BrokerReport report) {
        super(report,
                (cell) -> cell.startsWith("1.1 Информация о совершенных и исполненных сделках"),
                (cell) -> lastRowPattern.matcher(cell).lookingAt(),
                TransactionTableHeader.class);
    }

    @Override
    protected AbstractTransaction parseRow(TableRow row) {
        return super.parseRow(row);
    }

    enum TransactionTableHeader implements TableColumnDescription {
        TRADE_ID("номер", "сделки"),
        TIME("время"),
        DIRECTION("вид", "сделки"),
        SHORTNAME("сокращен", "наименова"),
        TICKER("код актива"),
        COUNT("количество"),
        AMOUNT("сумма", "без", "НКД"),
        ACCRUED_INTEREST("нкд"), // суммарно по всем бумагам
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
