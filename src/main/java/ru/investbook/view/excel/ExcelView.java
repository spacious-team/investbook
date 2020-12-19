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

package ru.investbook.view.excel;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.investbook.view.ViewFilter;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExcelView {
    private final List<ExcelTableView> excelTableViews;

    public void writeTo(XSSFWorkbook book) {
        ViewFilter filter = ViewFilter.get();
        List<ExcelTable> tables = excelTableViews.parallelStream()
                .map(excelTableView -> excelTableView.createExcelTables(filter))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        CellStyles styles = new CellStyles(book);
        tables.stream()
                .sorted(Comparator.comparing(t -> t.getCreator().getSheetOrder()))
                .forEach(table -> table.writeTo(book, styles));

        if (book.getNumberOfSheets() == 0) {
            book.createSheet("пустой отчет");
        }
    }
}
