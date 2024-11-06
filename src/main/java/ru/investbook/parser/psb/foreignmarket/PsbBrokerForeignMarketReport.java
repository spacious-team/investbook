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

package ru.investbook.parser.psb.foreignmarket;

import lombok.EqualsAndHashCode;
import nl.fountain.xelem.excel.Workbook;
import nl.fountain.xelem.lex.ExcelReader;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.xml.XmlReportPage;
import org.xml.sax.InputSource;
import ru.investbook.parser.AbstractBrokerReport;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.parser.psb.PsbBrokerReport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.HOURS;

@EqualsAndHashCode(callSuper = true)
public class PsbBrokerForeignMarketReport extends AbstractBrokerReport {

    private static final DateTimeFormatter dateFormatterWithSlash = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String PORTFOLIO_MARKER = "Договор №:";
    private static final String REPORT_DATE_MARKER = "ОТЧЕТ БРОКЕРА";

    public PsbBrokerForeignMarketReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(getBrokerReportAttributes(excelFileName, is), securityRegistrar);
    }

    private static Attributes getBrokerReportAttributes(String excelFileName, InputStream is) {
        Workbook book = getWorkbook(is);
        ReportPage reportPage = new XmlReportPage(book.getWorksheetAt(0));
        PsbBrokerReport.checkReportFormat(excelFileName, reportPage);
        return new Attributes(
                reportPage,
                excelFileName,
                getReportEndDateTime(reportPage),
                getPortfolio(reportPage));
    }

    private static Workbook getWorkbook(InputStream is) {
        try {
            ExcelReader reader = new ExcelReader();
            is = skipNewLines(is); // required by ExcelReader
            return reader.getWorkbook(new InputSource(is));
        } catch (Exception e) {
            throw new RuntimeException("Не смог открыть xml файл", e);
        }
    }

    private static InputStream skipNewLines(InputStream is) throws IOException {
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

    private static Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            String value = String.valueOf(reportPage.getNextColumnValue(REPORT_DATE_MARKER));
            String date = value.split(" ")[3];
            return convertToInstantWithRussianFormatAndMoscowZoneId(date)
                    .plus(LAST_TRADE_HOUR, HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        }
    }

    @Override
    public Instant convertToInstant(String value) {
        return convertXmlDateTimeToInstant(value, getReportZoneId(), super::convertToInstant);
    }

    protected static Instant convertToInstantWithRussianFormatAndMoscowZoneId(String value) {
        return convertXmlDateTimeToInstant(value,
                MOSCOW_ZONEID,
                AbstractBrokerReport::convertToInstantWithRussianFormatAndMoscowZoneId);
    }

    private static Instant convertXmlDateTimeToInstant(String value,
                                                       ZoneId zoneId,
                                                       Function<String, Instant> defaultConverter) {
        value = value.trim();
        if (value.contains("/")) {
            return LocalDate.parse(value, PsbBrokerForeignMarketReport.dateFormatterWithSlash)
                    .atStartOfDay(zoneId).toInstant();
        } else if (value.contains(":") && value.length() == 16) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(zoneId).toInstant();
        } else {
            return defaultConverter.apply(value);
        }
    }

    public static String convertToCurrency(String value) {
        return value.replace("RUR", "RUB"); // RUR (used till 1998) code in reports
    }

    @Override
    public void close() {
    }
}
