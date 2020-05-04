/*
 * Portfolio
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

package ru.portfolio.portfolio.view;

import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.portfolio.portfolio.view.excel.ExcelView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PortfolioViewRestController {
    private final ExcelView excelView;
    private final FileSystem jimfs = Jimfs.newFileSystem();

    @GetMapping("/portfolio")
    public void getExcelView(HttpServletResponse response) throws IOException {
        try {
            long t0 = System.nanoTime();
            String fileName = "portfolio.xlsx";
            Path path = jimfs.getPath(fileName);
            try (XSSFWorkbook book = new XSSFWorkbook()) {
                excelView.writeTo(book);
                book.write(Files.newOutputStream(path));
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            IOUtils.copy(Files.newInputStream(path), response.getOutputStream());
            log.info("Отчет {} сформирован за {}", path.getFileName(), Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String httpBody = Stream.of(sw.toString().split("\n"))
                    .collect(joining("</br>", "<b>Ошибка сборки отчета</b></br></br>", ""));
            response.setContentType("text/html; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(httpBody);
        }
        response.flushBuffer();
    }
}
