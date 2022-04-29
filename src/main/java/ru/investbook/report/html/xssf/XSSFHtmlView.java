/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.report.html.xssf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.investbook.report.ViewFilter;
import ru.investbook.report.excel.ExcelView;
import ru.investbook.report.html.HtmlCellStyles;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Slf4j
public class XSSFHtmlView {
    private final ExcelView excelView;

    public void create(OutputStream out, ViewFilter filter) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            excelView.writeTo(workbook, filter, new HtmlCellStyles(workbook));
            StringBuilder stringBuilder = new StringBuilder();
            ToHtml.create(workbook, stringBuilder);
            out.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
