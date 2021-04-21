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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.NAME_REGNUMBER_ISIN;
import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.SECTION;

public class VtbSecuritiesTable extends SingleAbstractReportTable<Security> {

    static final String TABLE_NAME = "Отчет об остатках ценных бумаг";
    static final String TABLE_FOOTER = "ИТОГО:";
    // security registration number -> isin
    private final Map<String, String> securityRegNumberToIsin = new HashMap<>();

    protected VtbSecuritiesTable(SingleBrokerReport report) {
        super(report, TABLE_NAME, TABLE_FOOTER, VtbSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Security> parseRowToCollection(TableRow row) {
        if (row.getCellValue(SECTION) == null) {
            return Collections.emptyList(); // sub-header row
        }
        String[] description = row.getStringCellValue(NAME_REGNUMBER_ISIN).split(",");
        String name = description[0].trim();
        String registrationNumber = description[1].toUpperCase().trim();
        String isin = description[2].toUpperCase().trim();
        securityRegNumberToIsin.put(registrationNumber, isin);
        return Collections.singleton(Security.builder()
                .id(isin)
                .name(name)
                .build());
    }

    public Map<String, String> getSecurityRegNumberToIsin() {
        initializeIfNeed();
        return securityRegNumberToIsin;
    }

    @Getter
    @RequiredArgsConstructor
    enum VtbSecuritiesTableHeader implements TableColumnDescription {
        NAME_REGNUMBER_ISIN("наименование", "гос. регистрации", "isin"),
        SECTION("площадка"),
        OUTGOING("исходящий остаток"),
        CURRENCY("валюта цены"),
        QUOTE("цена", "для облигаций"),
        FACE_VALUE("номинал"),
        ACCRUED_INTEREST("НКД в валюте номинала");

        private final TableColumn column;

        VtbSecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
