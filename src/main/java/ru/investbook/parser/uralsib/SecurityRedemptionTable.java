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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map.Entry;

import static ru.investbook.parser.uralsib.SecurityRedemptionTable.SecurityFlowTableHeader.*;

/**
 * Builds list of (security name; redemption date) tuples
 */
public class SecurityRedemptionTable extends SingleAbstractReportTable<Entry<String, Instant>> {
    static final String TABLE_NAME = "ДВИЖЕНИЕ ЦЕННЫХ БУМАГ ЗА ОТЧЕТНЫЙ ПЕРИОД";
    private static final String REDEMPTION_DESCRIPTION = "Списание ЦБ после погашения";

    public SecurityRedemptionTable(UralsibBrokerReport report) {
        super(report, TABLE_NAME, "", SecurityFlowTableHeader.class);
    }

    @Override
    protected Entry<String, Instant> parseRow(TableRow row) {
        return row.getStringCellValue(OPERATION).equalsIgnoreCase(REDEMPTION_DESCRIPTION) ?
                new AbstractMap.SimpleEntry<>(
                        row.getStringCellValue(NAME),
                        convertToInstant(row.getStringCellValue(DATE))) :
                null;
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
