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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import ru.investbook.view.ViewFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.lang.Math.min;
import static java.util.Comparator.comparing;

@Component
public class ExcelView {
    private final List<ExcelTableView> excelTableViews;

    public ExcelView(Collection<ExcelTableView> excelTableViews) {
        this.excelTableViews = excelTableViews.stream()
                .sorted(comparing(ExcelTableView::getSheetOrder))
                .collect(Collectors.toList());
    }

    public void writeTo(XSSFWorkbook book, ViewFilter filter) throws InterruptedException, ExecutionException {

        CellStyles styles = new CellStyles(book);
        ExecutorService tableWriterExecutor = Executors.newSingleThreadExecutor();
        Collection<Future<?>> sheetWriterFutures = new ArrayList<>();
        int cpuCnt = Runtime.getRuntime().availableProcessors();
        for (int idx = 0, delta = 1; idx < excelTableViews.size();) {
            int fromIndex = idx;
            idx += delta;
            delta = cpuCnt;
            int toIndex = min(idx, excelTableViews.size());
            final var tables = excelTableViews.subList(fromIndex, toIndex)
                            .parallelStream()
                            .map(excelTableView -> getExcelTables(excelTableView, filter))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
            Future<?> future = tableWriterExecutor.submit(() -> writeExcelTables(tables, book, styles));
            sheetWriterFutures.add(future);
        }

        for(Future<?> future : sheetWriterFutures) {
            future.get();
        }

        if (book.getNumberOfSheets() == 0) {
            book.createSheet("пустой отчет");
        }
    }

    private static Collection<ExcelTable> getExcelTables(ExcelTableView excelTableView, ViewFilter filter) {
        try {
            ViewFilter.set(filter);
            return excelTableView.createExcelTables();
        } finally {
            ViewFilter.remove();
        }
    }

    private static void writeExcelTables(List<ExcelTable> tables, Workbook book, CellStyles styles) {
        tables.stream()
                .sorted(comparing(t -> t.getCreator().getSheetOrder()))
                .forEach(table -> table.writeTo(book, styles));
    }
}
