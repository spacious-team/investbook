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

package ru.investbook.parser.vtb;

import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelTable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbSecurityFlowTable.VtbSecurityFlowTableHeader.*;

public class VtbSecurityDepositAndWithdrawalTable  extends AbstractReportTable<SecurityTransaction> {

    static final String TABLE_NAME = "Движение ценных бумаг";
    private static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    protected VtbSecurityDepositAndWithdrawalTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbSecurityFlowTable.VtbSecurityFlowTableHeader.class);
    }

    @Override
    protected Collection<SecurityTransaction> getRow(Table table, TableRow row) {
        String operation = table.getStringCellValueOrDefault(row, OPERATION, "").toLowerCase().trim();
        switch (operation) {
            case "конвертация цб":
            case "вывод цб": // указывался при сплите акций APPL 4:1 (count = +3) и TSLA 5:1 (count = +4)
            case "ввод цб": // догадка, нет примера отчета
                break;
            default:
                return Collections.emptyList();
        }

        String portfolio = getReport().getPortfolio();
        String isin = table.getStringCellValue(row, NAME_REGNUMBER_ISIN).split(",")[2].trim();
        Instant timestamp = ((ExcelTable) table).getDateCellValue(row, DATE).toInstant();
        Long transactionId = generateTransactionId(portfolio, timestamp, isin);
        return Collections.singleton(
                SecurityTransaction.builder()
                        .transactionId(transactionId)
                        .timestamp(timestamp)
                        .portfolio(portfolio)
                        .isin(isin)
                        .count(table.getIntCellValue(row, COUNT))
                        .value(BigDecimal.ZERO)
                        .accruedInterest(BigDecimal.ZERO)
                        .commission(BigDecimal.ZERO)
                        .valueCurrency("RUB")
                        .commissionCurrency("RUB")
                        .build());
    }

    private static Long generateTransactionId(String portfolio, Instant instant, String isin) {
        messageDigest.update((portfolio + isin).getBytes());
        return Math.abs(
                new BigInteger(1, messageDigest.digest(), 0, 8)
                        .longValue()
                        + instant.getEpochSecond());
    }
}
