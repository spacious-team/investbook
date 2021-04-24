/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.investbook.report.ViewFilter;
import ru.investbook.report.excel.ExcelView;
import ru.investbook.repository.PortfolioRepository;
import ru.investbook.web.model.ViewFilterModel;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static ru.investbook.web.ControllerHelper.getPortfolios;

@Controller
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Slf4j
public class InvestbookReportController {

    private static final String FROM_DATE_FIELD = "from-date";
    private static final String TO_DATE_FIELD = "to-date";
    private static final String REPORT_NAME = "investbook";
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    private final PortfolioRepository portfolioRepository;
    private final ExcelView excelView;
    private volatile int expectedFileSize = 0xFFFF;

    @GetMapping("/select-period")
    public String getPage(@ModelAttribute("viewFilter") ViewFilterModel viewFilter) {
        viewFilter.setPortfolios(getPortfolios(portfolioRepository));
        return "select-period";
    }

    @GetMapping
    public void buildInvestbookReportByGet(HttpServletResponse response) throws IOException {
        buildInvestbookReport(new ViewFilterModel(), response);
    }

    @PostMapping
    public void buildInvestbookReport(@ModelAttribute("viewFilter") ViewFilterModel viewFilter,
                                      HttpServletResponse response) throws IOException {
        try {
            long t0 = System.nanoTime();
            String fileName = sendExcelFile(viewFilter, response);
            log.info("Отчет '{}' сформирован за {}", fileName, Duration.ofNanos(System.nanoTime() - t0));
        } catch (Exception e) {
            log.error("Ошибка сборки отчета", e);
            sendErrorPage(response, e);
        }
        response.flushBuffer();
    }

    private String sendExcelFile(ViewFilterModel viewFilterModel, HttpServletResponse response)
            throws IOException, InterruptedException, ExecutionException {
        ViewFilter viewFilter = ViewFilter.of(viewFilterModel, () -> getPortfolios(portfolioRepository));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(expectedFileSize);
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            excelView.writeTo(book, viewFilter);
            book.write(outputStream);
            expectedFileSize = outputStream.size();
        }
        String fileName = getReportName(viewFilter);
        sendSuccessHeader(response, fileName);
        outputStream.writeTo(response.getOutputStream());
        return fileName;
    }

    private String getReportName(ViewFilter filter) {
        Instant toDate = filter.getToDate();
        if (toDate.isAfter(Instant.now())) {
            toDate = Instant.now();
        }
        return REPORT_NAME + " с " + dateFormatter.format(filter.getFromDate()) + " по " + dateFormatter.format(toDate) + ".xlsx";
    }

    private void sendSuccessHeader(HttpServletResponse response, String fileName) {
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        response.setHeader("Content-disposition", contentDisposition.toString());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private void sendErrorPage(HttpServletResponse response, Exception e) throws IOException {
        sendErrorHttpHeader(response);
        String httpBody = getErrorHttpBody(e);
        response.getWriter().write(httpBody);
    }

    private String getErrorHttpBody(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return Stream.of(sw.toString().split("\n"))
                .collect(joining("</br>", """
                        <b>Ошибка сборки отчета</b></br></br> <a href="/">[назад]</a>
                        <br/>
                        <span style="font-size: smaller; color: gray;">
                            Вы можете <a href="https://github.com/spacious-team/investbook/issues">сообщить</a>
                            об ошибке разработчикам или связаться с
                            <a href="https://t.me/investbook_support">технической поддержкой</a> 
                        </span>
                        </br></br> - 
                        """, ""));
    }

    private void sendErrorHttpHeader(HttpServletResponse response) {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
