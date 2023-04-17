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

package ru.investbook.parser.sber.cash_security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.sber.SecurityHelper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.sber.cash_security.SberSecurityDepsitAndWithdrawalTable.SberSecurityDepositAndWithdrawalTableHeader.*;

@Slf4j
public class SberSecurityDepsitAndWithdrawalTable extends AbstractReportTable<AbstractTransaction> {
    static final String FIRST_LINE = "Номер договора";
    @Getter
    private final Collection<Security> securities = new ArrayList<>();
    private final SberSecurityDepositBrokerReport report;

    protected SberSecurityDepsitAndWithdrawalTable(SberSecurityDepositBrokerReport report) {
        super(report, "Движение ЦБ", FIRST_LINE, null, SberSecurityDepositAndWithdrawalTableHeader.class);
        this.report = report;
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        if (!"Исполнено".equalsIgnoreCase(row.getStringCellValueOrDefault(STATUS, null))) {
            return null;
        } else if ("Погашение ценной бумаги".equalsIgnoreCase(row.getStringCellValueOrDefault(DESCRIPTION, null))) {
            return null;
        }
        String operation = row.getStringCellValue(OPERATION);
        int count;
        switch (operation) {
            case "Ввод ЦБ" -> count = row.getIntCellValue(COUNT);
            case "Вывод ЦБ" -> count = -row.getIntCellValue(COUNT);
            default -> {
                log.warn("Неизвестный тип операции: {} в отчете {}", operation, getReport());
                return null;
            }
        }
        String portfolio = row.getStringCellValue(PORTFOLIO);
        Instant instant = row.getInstantCellValue(DATE_TIME);
        Security security = SecurityHelper.getSecurity(
                row.getStringCellValue(CODE),
                row.getStringCellValueOrDefault(NAME, null),
                row.getStringCellValue(SECTION),
                null,
                report.getSecurityRegistrar());

        return SecurityTransaction.builder()
                .tradeId(generateTradeId(portfolio, instant, security.getId()))
                .timestamp(instant)
                .portfolio(portfolio)
                .security(security.getId())
                .count(count)
                .build();
    }

    private static String generateTradeId(String portfolio, Instant instant, Integer securityId) {
        String id = instant.getEpochSecond() + securityId + portfolio;
        return id.substring(0, Math.min(32, id.length()));
    }

    enum SberSecurityDepositAndWithdrawalTableHeader implements TableHeaderColumn {
        PORTFOLIO("Номер договора"),
        DATE_TIME("Дата исполнения поручения"),
        CODE("Код финансового инструмента"),
        NAME("Наименование финансового инструмента"),
        SECTION("Тип рынка"),
        OPERATION("Операция"),
        COUNT("Количество"),
        DESCRIPTION("Содержание операции"),
        STATUS("Статус");

        @Getter
        private final TableColumn column;
        SberSecurityDepositAndWithdrawalTableHeader(String words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
