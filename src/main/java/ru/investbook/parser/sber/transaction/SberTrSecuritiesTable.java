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

package ru.investbook.parser.sber.transaction;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.sber.transaction.SberTrSecurityTransactionTable.SberTransactionTableHeader;

import static ru.investbook.parser.sber.transaction.SberTrSecurityTransactionTable.SberTransactionTableHeader.NAME_AND_ISIN;

@Slf4j
public class SberTrSecuritiesTable extends AbstractReportTable<Security> {
    private static final String FIRST_LINE = "Номер договора";

    protected SberTrSecuritiesTable(BrokerReport report) {
        super(report, "Сделки", FIRST_LINE, null, SberTransactionTableHeader.class);
    }

    @Override
    protected Security parseRow(TableRow row) {
        String nameAndIsin = row.getStringCellValue(NAME_AND_ISIN); // format: "<name>\s*(<isin>)"
        int start = nameAndIsin.indexOf('(');
        int end = nameAndIsin.indexOf(')');
        String id = nameAndIsin.substring(start + 1, (end > -1) ? end : nameAndIsin.length());
        String name = (start == -1) ? null : nameAndIsin.substring(0, start).trim();
        return Security.builder()
                .id(id)
                .name(name)
                .build();
    }
}
