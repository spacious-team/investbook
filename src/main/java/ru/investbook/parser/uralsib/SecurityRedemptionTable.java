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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.uralsib.SecurityRedemptionTable.SecurityFlowTableHeader.*;

/**
 * Builds list of (security name; redemption date) tuples
 */
public class SecurityRedemptionTable extends AbstractReportTable<Map.Entry<String, Instant>> {
    static final String TABLE_NAME = "ДВИЖЕНИЕ ЦЕННЫХ БУМАГ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    private static final String REDEMPTION_DESCRIPTION = "Списание ЦБ после погашения";

    public SecurityRedemptionTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", SecurityFlowTableHeader.class);
    }

    @Override
    protected Collection<Map.Entry<String, Instant>> getRow(Table table, TableRow row) {
        return table.getStringCellValue(row, OPERATION).equalsIgnoreCase(REDEMPTION_DESCRIPTION) ?
                singletonList(new AbstractMap.SimpleEntry<>(
                        table.getStringCellValue(row, NAME),
                        convertToInstant(table.getStringCellValue(row, DATE)))) :
                emptyList();
    }

    enum SecurityFlowTableHeader implements TableColumnDescription {
        ID("№", "операции"),
        OPERATION("тип операции"),
        DATE("дата"),
        NAME("наименование"),
        CFI("номер гос. регистрации"),
        COUNT("количество цб");

        @Getter
        private final TableColumn column;

        SecurityFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
