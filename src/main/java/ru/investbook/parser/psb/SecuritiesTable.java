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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;
import org.spacious_team.broker.pojo.SecurityType;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.ISIN;
import static ru.investbook.parser.psb.SecuritiesTable.SecuritiesTableHeader.NAME;

@Slf4j
public class SecuritiesTable extends SingleAbstractReportTable<Security> {
    static final String TABLE_NAME = "Портфель на конец дня"; // старые отчеты, новые - "Портфель на конец дня на биржевом рынке"
    static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    static final String INVALID_TEXT = "Итого в валюте цены";

    public SecuritiesTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, SecuritiesTableHeader.class);
    }

    @Override
    protected Security parseRow(TableRow row) {
        if (row.rowContains(INVALID_TEXT)) {
            return null;
        }
        String isin = row.getStringCellValue(ISIN);
        SecurityBuilder security = Security.builder()
                .isin(isin)
                .name(row.getStringCellValue(NAME))
                .type(SecurityType.STOCK_OR_BOND);
        int securityId = getReport().getSecurityRegistrar().declareStockOrBondByIsin(isin, () -> security);
        return security.id(securityId).build();
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
        FACEUNIT("валюта бумаги"),
        CURRENCY("валюта цены");

        @Getter
        private final TableColumn column;

        SecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
