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
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static ru.investbook.parser.tinkoff.SecurityCodeAndIsinTable.SecurityAndCodeTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffBrokerReport.tablesLastRowPattern;

public class SecurityCodeAndIsinTable extends AbstractReportTable<Void> {

    private final Map<String, String> codeToIsin = new HashMap<>();
    private final Map<String, BigDecimal> codeToFaceValue = new HashMap<>();

    protected SecurityCodeAndIsinTable(BrokerReport report) {
        super(report,
                (cell) -> cell.startsWith("4.1 Информация о ценных бумагах"),
                (cell) -> tablesLastRowPattern.matcher(cell).lookingAt(),
                SecurityAndCodeTableHeader.class);
    }

    @Override
    protected Void parseRow(TableRow row) {
        String code = row.getStringCellValueOrDefault(CODE, null);
        if (StringUtils.hasLength(code) && !code.contains("Код актива")) { // exclude table's empty row
            codeToIsin.put(code, row.getStringCellValue(ISIN));
            BigDecimal faceValue = row.getBigDecimalCellValueOrDefault(FACE_VALUE, null);
            if (faceValue != null) {
                codeToFaceValue.put(code, faceValue);
            }
        }
        return null;
    }

    @NotNull
    public String getIsin(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToIsin.get(code));
    }

    @NotNull
    public BigDecimal getFaceValue(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToFaceValue.get(code));
    }

    protected enum SecurityAndCodeTableHeader implements TableColumnDescription {
        CODE("код", "актива"),
        ISIN("isin"),
        FACE_VALUE("Номинал");

        @Getter
        private final TableColumn column;

        SecurityAndCodeTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
