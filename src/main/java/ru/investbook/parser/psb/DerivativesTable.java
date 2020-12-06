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

package ru.investbook.parser.psb;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.CONTRACT;

public class DerivativesTable extends AbstractReportTable<Security> {

    public DerivativesTable(PsbBrokerReport report) {
        super(report, DerivativeCashFlowTable.TABLE2_NAME, DerivativeCashFlowTable.TABLE_END_TEXT,
                DerivativeCashFlowTable.ContractCountTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(Table table, TableRow row) {
        return Collections.singletonList(Security.builder()
                .id(table.getStringCellValue(row, CONTRACT))
                .build());
    }
}
