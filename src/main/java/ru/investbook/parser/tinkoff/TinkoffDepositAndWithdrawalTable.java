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

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toMap;
import static ru.investbook.parser.tinkoff.TinkoffDepositAndWithdrawalTable.SecurityDepositAndWithdrawalTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.declareSecurity;

@Slf4j
public class TinkoffDepositAndWithdrawalTable extends SingleAbstractReportTable<SecurityTransaction> {

    // security id -> abs(buy/cell count)
    private final Map<Integer, Integer> transactionDeposit;
    private final Map<Integer, Integer> transactionWithdrawal;
    private final SecurityCodeAndIsinTable codeAndIsin;

    protected TinkoffDepositAndWithdrawalTable(SingleBrokerReport report,
                                               ReportTable<AbstractTransaction> transactions,
                                               SecurityCodeAndIsinTable codeAndIsin) {
        super(report,
                cell -> cell.startsWith("3.1 Движение по ценным бумагам инвестора"),
                cell -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                TinkoffDepositAndWithdrawalTable.SecurityDepositAndWithdrawalTableHeader.class);
        this.codeAndIsin = codeAndIsin;
        this.transactionDeposit = getCounter(transactions, t -> t.getCount() > 0);
        this.transactionWithdrawal = getCounter(transactions, t -> t.getCount() < 0);
    }

    @NotNull
    private static Map<Integer, Integer> getCounter(ReportTable<AbstractTransaction> transactions,
                                                    Predicate<AbstractTransaction> filter) {
        return transactions.getData()
                .stream()
                .filter(filter)
                .collect(toMap(
                        AbstractTransaction::getSecurity,
                        t -> Math.abs(t.getCount()),
                        Integer::sum));
    }

    @Override
    protected Collection<SecurityTransaction> parseRowToCollection(TableRow row) {
        int totalDeposit, totalWithdrawal;
        if ((totalDeposit = row.getIntCellValueOrDefault(DEPOSIT, -1)) <= 0 ||
                (totalWithdrawal = row.getIntCellValueOrDefault(WITHDRAWAL, -1)) <= 0) {
            return Collections.emptyList();
        }
        int securityId = getSecurityId(row);
        int splitDeposit = totalDeposit - transactionDeposit.getOrDefault(securityId, 0);
        int splitWithdrawal = totalWithdrawal - transactionWithdrawal.getOrDefault(securityId, 0);
        if (splitDeposit <= 0 || splitWithdrawal <= 0) {
            return Collections.emptyList();
        }
        SecurityTransaction.SecurityTransactionBuilder<?, ?> builder = SecurityTransaction.builder()
                .portfolio(getReport().getPortfolio())
                .security(securityId)
                .timestamp(getReport().getReportEndDateTime());
        SecurityTransaction deposit = builder
                .tradeId(getTradeId(securityId, "d"))
                .count(splitDeposit)
                .build();
        SecurityTransaction withdrawal = builder
                .tradeId(getTradeId(securityId, "w"))
                .count(-splitWithdrawal)
                .build();
        LocalDate splitDate = LocalDate.ofInstant(getReport().getReportEndDateTime(), getReport().getReportZoneId());
        log.warn("В отчете брокера для акции {} ({}) для сплита {}:{} не указана дата, " +
                        "использую конечную дату отчета {}, " +
                        "исправьте дату через Формы",
                row.getStringCellValue(SHORT_NAME), row.getStringCellValue(CODE), splitDeposit, splitWithdrawal, splitDate);
        return List.of(deposit, withdrawal);
    }

    private int getSecurityId(TableRow row) {
        String code = row.getStringCellValue(CODE);
        String shortName = row.getStringCellValue(SHORT_NAME);
        SecurityType securityType = codeAndIsin.getSecurityType(code, shortName);
        Security security = TinkoffSecurityTransactionTableHelper.getSecurity(
                code,
                codeAndIsin,
                shortName,
                securityType);
        return declareSecurity(security, getReport().getSecurityRegistrar());
    }

    private String getTradeId(int securityId, String marker) {
        String tradeId = securityId +
                marker +
                getReport().getReportEndDateTime().getEpochSecond() +
                getReport().getPortfolio().replaceAll(" ", "");
        return tradeId.substring(0, Math.min(32, tradeId.length()));
    }

    protected enum SecurityDepositAndWithdrawalTableHeader implements TableHeaderColumn {
        SHORT_NAME("Сокращенное", "наименование"),
        CODE("код", "актива"),
        DEPOSIT("зачисление"),
        WITHDRAWAL("списание");

        @Getter
        private final TableColumn column;

        SecurityDepositAndWithdrawalTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
