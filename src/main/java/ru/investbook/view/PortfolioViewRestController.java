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

package ru.investbook.view;

import com.google.common.jimfs.Jimfs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.investbook.view.excel.ExcelView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PortfolioViewRestController {

    private static final String FROM_DATE_FIELD = "from-date";
    private static final String TO_DATE_FIELD = "to-date";
    private static final String REPORT_NAME = "investbook";
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    private final ExcelView excelView;
    private final FileSystem jimfs = Jimfs.newFileSystem();

    @PostMapping("/portfolio")
    public void getExcelView(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long t0 = System.nanoTime();
            ViewFilter.set(getViewFilter(request));
            Path path = jimfs.getPath(getReportName(ViewFilter.get()));
            try (XSSFWorkbook book = new XSSFWorkbook()) {
                excelView.writeTo(book);
                book.write(Files.newOutputStream(path));
            }
            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(String.valueOf(path.getFileName()), StandardCharsets.UTF_8)
                    .build();
            response.setHeader("Content-disposition", contentDisposition.toString());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            IOUtils.copy(Files.newInputStream(path), response.getOutputStream());
            log.info("Отчет '{}' сформирован за {}", path.getFileName(), Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            log.error("Ошибка сборки отчета", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String httpBody = Stream.of(sw.toString().split("\n"))
                    .collect(joining("</br>", "<b>Ошибка сборки отчета</b></br></br> <a href=\"/\">[назад]</a><br/>" +
                                    "<span style=\"font-size: smaller; color: gray;\">Вы можете " +
                                    "<a href=\"https://github.com/spacious-team/investbook/issues\">сообщить</a> об ошибке " +
                                    "разработчикам</span></br></br> - ", ""));
            response.setContentType("text/html; charset=utf-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(httpBody);
        } finally {
            ViewFilter.remove();
        }
        response.flushBuffer();
    }

    private ViewFilter getViewFilter(HttpServletRequest request) {
        ViewFilter.ViewFilterBuilder viewFilterBuilder = ViewFilter.builder();
        setViewFilterDate(request, FROM_DATE_FIELD, viewFilterBuilder::fromDate);
        setViewFilterDate(request, TO_DATE_FIELD, viewFilterBuilder::toDate);
        return viewFilterBuilder.build();
    }

    private void setViewFilterDate(HttpServletRequest request, String dateField, Consumer<Instant> setter) {
        try {
            setter.accept(LocalDate.parse(request.getParameter(dateField), DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant());
        } catch (Exception ignore) {
        }
    }

    private String getReportName(ViewFilter filter) {
        Instant toDate = filter.getToDate();
        if (toDate.isAfter(Instant.now())) {
            toDate = Instant.now();
        }
        return REPORT_NAME + " с " + dateFormatter.format(filter.getFromDate()) + " по " + dateFormatter.format(toDate) + ".xlsx";
    }
}
