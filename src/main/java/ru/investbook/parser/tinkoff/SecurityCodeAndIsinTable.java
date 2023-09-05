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
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.OptionalTableColumn;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.SecurityCodeAndIsinTable.SecurityAndCodeTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffBrokerReport.tablesLastRowPattern;

public class SecurityCodeAndIsinTable extends AbstractReportTable<Void> {

    private final Map<String, String> codeToIsin = new HashMap<>();
    private final Map<String, SecurityType> codeToType = new HashMap<>();
    private final Map<String, BigDecimal> codeToFaceValue = new HashMap<>();
    private final Map<String, String> shortNameToCode = new HashMap<>();

    protected SecurityCodeAndIsinTable(BrokerReport report) {
        super(report,
                cell -> cell.startsWith("4.1 Информация о ценных бумагах"),
                cell -> tablesLastRowPattern.matcher(cell).lookingAt(),
                SecurityAndCodeTableHeader.class);
    }

    @Override
    protected Void parseRow(TableRow row) {
        String code = row.getStringCellValueOrDefault(CODE, null);
        if (hasLength(code) && !code.contains("Код актива")) { // exclude table's empty row
            // если колонка ISIN отсутствует, то ISIN используется в отчете вместо кода (SBERP)
            String isin = row.getStringCellValueOrDefault(ISIN, code);
            codeToIsin.put(code, isin);

            String type = row.getStringCellValueOrDefault(TYPE, "").toLowerCase();
            SecurityType securityType;
            if (type.contains("акци") || type.contains("депозитарн")) {
                securityType = SecurityType.STOCK;
            } else if (type.contains("обл ") || type.contains("облигаци")) {
                securityType = SecurityType.BOND;
            } else {
                securityType = SecurityType.STOCK_OR_BOND;
            }
            codeToType.put(code, securityType);

            BigDecimal faceValue = row.getBigDecimalCellValueOrDefault(FACE_VALUE, null);
            if (faceValue != null) {
                codeToFaceValue.put(code, faceValue);
            }

            String shortName = row.getStringCellValueOrDefault(SHORT_NAME, null);
            if (hasLength(shortName)) {
                shortNameToCode.put(shortName, code);
            }
        }
        return null;
    }

    @NotNull
    public String getIsin(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToIsin.get(code), "Не найден ISIN");
    }

    @NotNull
    public SecurityType getSecurityType(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToType.get(code), "Не найден тип ценной бумаги");
    }

    @NotNull
    public BigDecimal getFaceValue(String code) {
        initializeIfNeed();
        return Objects.requireNonNull(codeToFaceValue.get(code), "Не найдена номинальная стоимость облигации");
    }

    @NotNull
    public String getCode(String shortName) {
        initializeIfNeed();
        return Objects.requireNonNull(shortNameToCode.get(shortName), "Не найден код бумаги");
    }

    @RequiredArgsConstructor
    protected enum SecurityAndCodeTableHeader implements TableHeaderColumn {
        SHORT_NAME("Сокращенное", "наименование"),
        CODE("код", "актива"),  // код (SBERP) или ISIN
        ISIN(optional("isin")), // может отсутствовать, если колонка CODE заполняется ISIN
        TYPE("^Тип$"),
        FACE_VALUE("Номинал");

        @Getter
        private final TableColumn column;

        SecurityAndCodeTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }

        static OptionalTableColumn optional(String... words) {
            return OptionalTableColumn.of(PatternTableColumn.of(words));
        }
    }
}
