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
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.investbook.parser.vtb.VtbSecurityFlowTable.VtbSecurityFlowTableHeader.NAME_REGNUMBER_ISIN;

public class VtbSecurityFlowTable extends AbstractReportTable<Security> {

    // security registration number -> isin
    private final Map<String, String> securityRegNumberToIsin = new HashMap<>();

    protected VtbSecurityFlowTable(BrokerReport report) {
        super(report, VtbSecurityDepositAndWithdrawalTable.TABLE_NAME, null, VtbSecurityFlowTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(TableRow row) {
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
    enum VtbSecurityFlowTableHeader implements TableColumnDescription {
        NAME_REGNUMBER_ISIN("наименование", "гос. регистрации", "isin"),
        DATE("дата"),
        COUNT("количество"),
        OPERATION("тип операции"),
        DESCRIPTION("комментарий");

        private final TableColumn column;

        VtbSecurityFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
