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

package ru.investbook.parser.sber.cash_security;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.sber.SecurityHelper;
import ru.investbook.parser.sber.cash_security.SberSecurityDepsitAndWithdrawalTable.SberSecurityDepositAndWithdrawalTableHeader;

import static ru.investbook.parser.sber.cash_security.SberSecurityDepsitAndWithdrawalTable.FIRST_LINE;
import static ru.investbook.parser.sber.cash_security.SberSecurityDepsitAndWithdrawalTable.SberSecurityDepositAndWithdrawalTableHeader.*;

@Slf4j
public class SberSecuritySecuritiesTable extends AbstractReportTable<Security> {

    protected SberSecuritySecuritiesTable(BrokerReport report) {
        super(report, "Движение ЦБ", FIRST_LINE, null, SberSecurityDepositAndWithdrawalTableHeader.class);
    }

    @Override
    protected Security parseRow(TableRow row) {
        String code = row.getStringCellValue(CODE);
        return Security.builder()
                .id(SecurityHelper.getSecurityId(code, row.getStringCellValue(SECTION)))
                .name(row.getStringCellValueOrDefault(NAME, null))
                .build();
    }
}
