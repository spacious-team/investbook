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

import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.NAME_AND_ISIN;
import static ru.investbook.parser.vtb.VtbSecuritiesTable.VtbSecuritiesTableHeader.SECTION;

public class VtbSecuritiesTable extends AbstractReportTable<Security> {

    private static final String TABLE_NAME = "Отчет об остатках ценных бумаг";
    private static final String TABLE_FOOTER = "ИТОГО:";

    protected VtbSecuritiesTable(BrokerReport report) {
        super(report, TABLE_NAME, TABLE_FOOTER, VtbSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(Table table, TableRow row) {
        if (table.getCellValue(row, SECTION) == null) {
            return Collections.emptyList(); // sub-header row
        }
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
    enum VtbSecuritiesTableHeader implements TableColumnDescription {
        NAME_AND_ISIN("наименование", "isin"),
        SECTION("площадка"),
        OUTGOING("плановый исходящий остаток"),
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
