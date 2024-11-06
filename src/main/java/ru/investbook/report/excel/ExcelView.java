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

import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.investbook.report.ViewFilter;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    @Transactional(readOnly = true)
    @SneakyThrows
    public void create(OutputStream out, ViewFilter filter) {
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            CellStyles styles = new CellStyles(book);
            writeTo(book, filter, styles);
            book.write(out);
        }
    }

    @Transactional(readOnly = true)
    public void writeTo(Workbook book, ViewFilter filter, CellStyles styles) {
        try (ExecutorService tableWriterExecutor = Executors.newSingleThreadExecutor()) {
            int cpuCnt = Runtime.getRuntime().availableProcessors();
            List<ExcelTableView> usedExcelTableViews = getExcelTableViews(filter);
            for (int idx = 0, delta = 1; idx < usedExcelTableViews.size(); ) {
                int fromIndex = idx;
                idx += delta;
                delta = cpuCnt;
                int toIndex = min(idx, usedExcelTableViews.size());
                List<ExcelTable> tables = usedExcelTableViews.subList(fromIndex, toIndex)
                        .parallelStream()
                        .map(excelTableView -> getExcelTables(excelTableView, filter))
                        .flatMap(Collection::stream)
                        .toList();
                tableWriterExecutor.submit(() -> writeExcelTables(tables, book, styles));
            }
        }

        if (book.getNumberOfSheets() == 0) {
            book.createSheet("пустой отчет");
        }
    }

    private List<ExcelTableView> getExcelTableViews(ViewFilter filter) {
        if (filter.isShowDetails()) {
            return excelTableViews;
        } else {
            return excelTableViews.stream()
                    .filter(ExcelTableView::isSummaryView)
                    .collect(Collectors.toList());
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
