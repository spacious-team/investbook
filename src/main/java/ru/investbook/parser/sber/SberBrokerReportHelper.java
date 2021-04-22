/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.sber;

import lombok.Getter;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;

import java.util.Set;
import java.util.stream.Collectors;

public class SberBrokerReportHelper {

    public static Set<String> findPortfolios(ReportPage reportPage) {
        return reportPage.createNameless("Номер договора", PortfolioTableHeader.class)
                .stream()
                .map(row -> row.getStringCellValue(PortfolioTableHeader.PORTFOLIO))
                .collect(Collectors.toSet());
    }

    private enum PortfolioTableHeader implements TableColumnDescription {
        PORTFOLIO("Номер договора");

        @Getter
        private final TableColumn column;

        PortfolioTableHeader(String words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
