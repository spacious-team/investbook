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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.NAME_REGNUMBER_ISIN;
import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.SECTION;

public class VtbSecuritiesTable extends SingleAbstractReportTable<Security> {

    static final String TABLE_NAME = "Отчет об остатках ценных бумаг";
    static final String TABLE_FOOTER = "ИТОГО:";
    // security registration number -> Security
    private final Map<String, Security> regNumberToSecurity = new HashMap<>();

    protected VtbSecuritiesTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, TABLE_FOOTER, VtbSecuritiesTableHeader.class);
    }

    @Override
    protected @Nullable Security parseRow(TableRow row) {
        if (row.getCellValue(SECTION) == null) {
            return null; // sub-header row
        }
        String description = row.getStringCellValue(NAME_REGNUMBER_ISIN);
        Security security = VtbReportHelper.getSecurity(description);
        String isin = requireNonNull(security.getIsin());
        int securityId = getReport().getSecurityRegistrar().declareStockOrBondByIsin(isin, security::toBuilder);
        security = security.toBuilder().id(securityId).build();
        String registrationNumber = description.split(",")[1].toUpperCase().trim();
        regNumberToSecurity.put(registrationNumber, security);
        return security;
    }

    public Map<String, Security> getRegNumberToSecurity() {
        initializeIfNeed();
        return regNumberToSecurity;
    }

    @Getter
    @RequiredArgsConstructor
    enum VtbSecuritiesTableHeader implements TableHeaderColumn {
        NAME_REGNUMBER_ISIN("наименование", "гос. регистрации", "isin"),
        SECTION("площадка"),
        OUTGOING("исходящий остаток"),
        CURRENCY("валюта цены", "номинала для облигаций"),
        QUOTE("цена", "для облигаций"),
        FACE_VALUE("номинал"),
        ACCRUED_INTEREST("НКД в валюте номинала");

        private final TableColumn column;

        VtbSecuritiesTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
