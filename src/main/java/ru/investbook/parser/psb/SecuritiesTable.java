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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.ISIN;
import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.NAME;

@Slf4j
public class SecuritiesTable extends AbstractReportTable<Security> {
    static final String TABLE_NAME = "Портфель на конец дня на биржевом рынке";
    static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    static final String INVALID_TEXT = "Итого в валюте цены";

    public SecuritiesTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, SecuritiesTableHeader.class);
    }

    @Override
    protected Collection<Security> getRow(Table table, TableRow row) {
        return row.rowContains(INVALID_TEXT) ?
                emptyList() :
                Collections.singletonList(Security.builder()
                        .isin(table.getStringCellValue(row, ISIN))
                        .name(table.getStringCellValue(row, NAME))
                        .build());
    }

    enum SecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin"),
        OUTGOING("исходящий", "остаток"),
        BUY("зачислено"),
        CELL("списано"),
        QUOTE("цена*", "для обл"),
        AMOUNT("оценочная стоимость в валюте цены"),
        ACCRUED_INTEREST("нкд"),
        CURRENCY("валюта цены");

        @Getter
        private final TableColumn column;
        SecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
