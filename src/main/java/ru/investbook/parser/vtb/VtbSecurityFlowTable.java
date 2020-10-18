/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.investbook.parser.*;
import ru.investbook.parser.table.Table;
import ru.investbook.parser.table.TableRow;
import ru.investbook.pojo.Security;

import java.util.Collection;
import java.util.Collections;

import static ru.investbook.parser.vtb.VtbSecurityFlowTable.VtbSecurityFlowTableHeader.NAME_AND_ISIN;

public class VtbSecurityFlowTable extends AbstractReportTable<Security> {

    protected VtbSecurityFlowTable(BrokerReport report) {
        super(report, VtbSecurityDepositAndWithdrawalTable.TABLE_NAME, null, VtbSecurityFlowTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(Table table, TableRow row) {
        String[] nameAndIsin = table.getStringCellValue(row, NAME_AND_ISIN).split(",");
        String name = nameAndIsin[0].trim();
        String isin = nameAndIsin[2].toUpperCase().trim();
        return Collections.singleton(Security.builder()
                .isin(isin)
                .name(name)
                .build());
    }

    @Getter
    @RequiredArgsConstructor
    enum VtbSecurityFlowTableHeader implements TableColumnDescription {
        NAME_AND_ISIN("наименование", "isin"),
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
