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

package ru.investbook.parser.psb.foreignmarket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.fountain.xelem.excel.Workbook;
import nl.fountain.xelem.lex.ExcelReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import ru.investbook.parser.BrokerReport;
import ru.investbook.parser.psb.PsbBrokerReport;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.xml.XmlReportPage;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode(of = "path")
public class PsbBrokerForeignMarketReport implements BrokerReport {
    private static final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final String PORTFOLIO_MARKER = "Договор №:";
    private static final String REPORT_DATE_MARKER = "ОТЧЕТ БРОКЕРА";

    @Getter
    private final ReportPage reportPage;
    @Getter
    private final String portfolio;
    @Getter
    private final Path path;
    @Getter
    private final Instant reportDate;

    public PsbBrokerForeignMarketReport(String excelFileName) throws IOException, ParserConfigurationException, SAXException {
        this(Paths.get(excelFileName));
    }

    public PsbBrokerForeignMarketReport(Path report) throws IOException, ParserConfigurationException, SAXException {
        this(report.getFileName().toString(), Files.newInputStream(report));
    }

    public PsbBrokerForeignMarketReport(String excelFileName, InputStream is) throws IOException, ParserConfigurationException, SAXException {
        Workbook book = getWorkbook(is);
        this.reportPage = new XmlReportPage(book.getWorksheetAt(0));
        this.portfolio = getPortfolio(this.reportPage);
        this.reportDate = getReportDate(this.reportPage);
        this.path = Paths.get(excelFileName);
    }

    private Workbook getWorkbook(InputStream is) throws IOException, SAXException, ParserConfigurationException {
        ExcelReader reader = new ExcelReader();
        is = skeepNewLines(is); // required by ExcelReader
        return reader.getWorkbook(new InputSource(is));
    }

    private InputStream skeepNewLines(InputStream is) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(is.readAllBytes());
        int symbol;
        do {
            bais.mark(8);
            symbol = bais.read();
        } while (symbol == '\n' || symbol == '\r');
        bais.reset();
        return bais;
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            String value = PsbBrokerReport.getValueByMarker(reportPage, PORTFOLIO_MARKER);
            if (value != null) {
                return value.contains("/") ? value.split("/")[0] : value;
            }
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете");
        }
    }

    private Instant getReportDate(ReportPage reportPage) {
        try {
            String value = PsbBrokerReport.getValueByMarker(reportPage, REPORT_DATE_MARKER);
            if (value != null) {
                return convertToInstant(value.split(" ")[3]);
            }
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска даты отчета");
        }
    }

    public Instant convertToInstant(String value) {
        if (value.contains(":")) {
            return LocalDateTime.parse(value, PsbBrokerForeignMarketReport.dateTimeFormatter).atZone(PsbBrokerForeignMarketReport.zoneId).toInstant();
        } else {
            return LocalDate.parse(value, PsbBrokerForeignMarketReport.dateFormatter).atStartOfDay(PsbBrokerForeignMarketReport.zoneId).toInstant();
        }
    }

    @Override
    public void close() {
    }
}
