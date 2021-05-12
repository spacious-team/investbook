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

package ru.investbook.parser.sber.cash_security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.sber.cash_security.SberSecurityDepsitAndWithdrawalTable.SberSecurityDepositAndWithdrawalTableHeader.*;

@Slf4j
public class SberSecurityDepsitAndWithdrawalTable extends AbstractReportTable<SecurityTransaction> {
    static final String FIRST_LINE = "Номер договора";
    @Getter
    private final Collection<Security> securities = new ArrayList<>();

    protected SberSecurityDepsitAndWithdrawalTable(SberSecurityDepositBrokerReport report) {
        super(report, "Движение ЦБ", FIRST_LINE, null, SberSecurityDepositAndWithdrawalTableHeader.class);
    }

    @Override
    protected SecurityTransaction parseRow(TableRow row) {
        if (!"Исполнено".equalsIgnoreCase(row.getStringCellValueOrDefault(STATUS, null))) {
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
        String code = row.getStringCellValue(CODE);
        return SecurityTransaction.builder()
                .transactionId(generateTransactionId(portfolio, instant, code))
                .timestamp(instant)
                .portfolio(portfolio)
                .security(code)
                .count(count)
                .build();
    }

    private static String generateTransactionId(String portfolio, Instant instant, String code) {
        String id = instant.getEpochSecond() + code + portfolio;
        return id.substring(0, Math.min(32, id.length()));
    }

    enum SberSecurityDepositAndWithdrawalTableHeader implements TableColumnDescription {
        PORTFOLIO("Номер договора"),
        DATE_TIME("Дата исполнения поручения"),
        CODE("Код финансового инструмента"),
        NAME("Наименование финансового инструмента"),
        OPERATION("Операция"),
        COUNT("Количество"),
        DESCRIPTION("Содержание операции"),
        STATUS("Статус");

        @Getter
        private final TableColumn column;
        SberSecurityDepositAndWithdrawalTableHeader(String words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
