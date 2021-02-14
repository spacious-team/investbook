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

package ru.investbook.parser.uralsib;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.uralsib.SecuritiesTable.ReportSecurityInformation;

import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.investbook.parser.uralsib.SecuritiesTable.SecuritiesTableHeader.*;

@Slf4j
public class SecuritiesTable extends AbstractReportTable<ReportSecurityInformation> {
    static final String TABLE_NAME = "СОСТОЯНИЕ ПОРТФЕЛЯ ЦЕННЫХ БУМАГ";
    static final String TABLE_END_TEXT = "Итого:";

    public SecuritiesTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, SecuritiesTableHeader.class);
    }

    @Override
    protected Collection<ReportSecurityInformation> getRow(Table table, TableRow row) {
        Security security = Security.builder()
                .id(table.getStringCellValue(row, ISIN))
                .name(table.getStringCellValue(row, NAME))
                .build();
        return singletonList(ReportSecurityInformation.builder()
                .security(security)
                .cfi(table.getStringCellValue(row, CFI))
                .incomingCount(table.getIntCellValue(row, INCOMING_COUNT))
                .build());
    }

    enum SecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin"),
        CFI("cfi"),
        INCOMING_COUNT("количество", "на начало периода"),
        OUTGOING_COUNT("количество", "на конец периода"),
        QUOTE("цена закрытия одной цб без учета нкд"),
        AMOUNT("Стоимость позиции по цене закрытия"), // в рублях для СПБ биржи
        ACCRUED_INTEREST("^нкд$");

        @Getter
        private final TableColumn column;
        SecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @ToString
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    static class ReportSecurityInformation {
        private final Security security;
        private final String cfi;
        private final int incomingCount;
    }
}
