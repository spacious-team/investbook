/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import static ru.investbook.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.CONTRACT;

public class DerivativesTable extends SingleAbstractReportTable<Security> {

    public DerivativesTable(PsbBrokerReport report) {
        super(report, DerivativeCashFlowTable.TABLE2_NAME, DerivativeCashFlowTable.TABLE_END_TEXT,
                DerivativeCashFlowTable.ContractCountTableHeader.class);
    }

    @Override
    protected Security parseRow(TableRow row) {
        String contract = row.getStringCellValue(CONTRACT);
        String securityId = getReport().getSecurityRegistrar().declareDerivative(contract);
        return Security.builder()
                .id(securityId)
                .type(SecurityType.DERIVATIVE)
                .build();
    }
}
