/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.report.excel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.broker.pojo.Portfolio;
import ru.investbook.report.Table;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ExcelTable {
    private final Portfolio portfolio;
    private final String sheetName;
    private final Table table;
    private final ExcelTableView creator;

    static ExcelTable of(String sheetName, Table table, ExcelTableView creator) {
        log.debug("Обработка данных для таблицы '{}' завершена", sheetName);
        return new ExcelTable(null, sheetName, table, creator);
    }

    static ExcelTable of(Portfolio portfolio, String sheetName, Table table, ExcelTableView creator) {
        log.debug("Обработка данных для таблицы '{}' завершена", sheetName);
        return new ExcelTable(portfolio, sheetName, table, creator);
    }

    void writeTo(Workbook book, CellStyles cellStyles) {
        creator.createSheet(portfolio, book, sheetName, table, cellStyles);
    }
}
