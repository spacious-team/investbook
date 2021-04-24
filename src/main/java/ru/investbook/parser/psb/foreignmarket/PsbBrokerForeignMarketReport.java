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

package ru.investbook.parser.psb.foreignmarket;

import lombok.EqualsAndHashCode;
import nl.fountain.xelem.excel.Workbook;
import nl.fountain.xelem.lex.ExcelReader;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.xml.XmlReportPage;
import org.xml.sax.InputSource;
import ru.investbook.parser.AbstractBrokerReport;
import ru.investbook.parser.psb.PsbBrokerReport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@EqualsAndHashCode(callSuper = true)
public class PsbBrokerForeignMarketReport extends AbstractBrokerReport {

    private static final DateTimeFormatter dateFormatterWithSlash = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String PORTFOLIO_MARKER = "Договор №:";
    private static final String REPORT_DATE_MARKER = "ОТЧЕТ БРОКЕРА";

    public PsbBrokerForeignMarketReport(String excelFileName, InputStream is) {
        Workbook book = getWorkbook(is);
        ReportPage reportPage = new XmlReportPage(book.getWorksheetAt(0));
        PsbBrokerReport.checkReportFormat(excelFileName, reportPage);
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    private static Workbook getWorkbook(InputStream is) {
        try {
            ExcelReader reader = new ExcelReader();
            is = skeepNewLines(is); // required by ExcelReader
            return reader.getWorkbook(new InputSource(is));
        } catch (Exception e) {
            throw new RuntimeException("Не смог открыть xml файл", e);
        }
    }

    private static InputStream skeepNewLines(InputStream is) throws IOException {
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
            String value = String.valueOf(reportPage.getNextColumnValue(PORTFOLIO_MARKER));
            return (value.contains("/") ? value.split("/")[0] : value) + "V";
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            String value = String.valueOf(reportPage.getNextColumnValue(REPORT_DATE_MARKER));
            return convertToInstant(value.split(" ")[3])
                        .plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        }
    }

    @Override
    public Instant convertToInstant(String value) {
        value = value.trim();
        if (value.contains("/")) {
            return LocalDate.parse(value, PsbBrokerForeignMarketReport.dateFormatterWithSlash)
                    .atStartOfDay(getReportZoneId()).toInstant();
        } else if (value.contains(":") && value.length() == 16) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(getReportZoneId()).toInstant();
        } else {
            return super.convertToInstant(value);
        }
    }

    @Override
    public void close() {
    }
}
