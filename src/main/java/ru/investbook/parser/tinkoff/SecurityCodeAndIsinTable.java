/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ru.investbook.parser.tinkoff.TinkoffBrokerReport.tablesLastRowPattern;

public class SecurityCodeAndIsinTable extends AbstractReportTable<Void> {

    private final Map<String, String> codeToIsin = new HashMap<>();

    protected SecurityCodeAndIsinTable(BrokerReport report) {
        super(report,
                (cell) -> cell.startsWith("4.1 Информация о ценных бумагах"),
                (cell) -> tablesLastRowPattern.matcher(cell).lookingAt(),
                SecurityAndCodeTableHeader.class); // TODO между наименованием страницы и заголовком может быть строка с указанием номера страницы 3/5
    }

    @Override
    protected Void parseRow(TableRow row) {
        String code = row.getStringCellValueOrDefault(SecurityAndCodeTableHeader.CODE, null);
        if (code != null) { // exclude table's empty row
            String isin = row.getStringCellValue(SecurityAndCodeTableHeader.ISIN);
            codeToIsin.put(code, isin);
        }
        return null;
    }

    @NotNull
    public String getIsin(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToIsin.get(code));
    }

    protected enum SecurityAndCodeTableHeader implements TableColumnDescription {
        CODE("код", "актива"),
        ISIN("isin");

        @Getter
        private final TableColumn column;

        SecurityAndCodeTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
