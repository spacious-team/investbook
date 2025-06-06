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

package ru.investbook.parser.psb;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;
import ru.investbook.parser.SecurityRegistrar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.HOURS;

@EqualsAndHashCode(callSuper = true)
public class PsbBrokerReport extends AbstractExcelBrokerReport {
    public static final String UNIQ_TEXT = "Брокер: ПАО \"Промсвязьбанк\"";
    private static final String PORTFOLIO_MARKER = "Договор №:";
    private static final String REPORT_DATE_MARKER = "ОТЧЕТ БРОКЕРА";

    public PsbBrokerReport(String excelFileName, SecurityRegistrar securityRegistrar) throws IOException {
        this(Paths.get(excelFileName), securityRegistrar);
    }

    public PsbBrokerReport(Path report, SecurityRegistrar securityRegistrar) throws IOException {
        this(getFileName(report), Files.newInputStream(report), securityRegistrar);
    }

    public PsbBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(getBrokerReportAttributes(excelFileName, is), securityRegistrar);
    }

    @SuppressWarnings("nullness")
    private static String getFileName(Path path) {
        return path.getFileName().toString();
    }

    private static ExcelAttributes getBrokerReportAttributes(String excelFileName, InputStream is) {
        Workbook workbook = getWorkBook(excelFileName, is);
        ReportPage reportPage = new ExcelSheet(workbook.getSheetAt(0));
        checkReportFormat(excelFileName, reportPage);
        Attributes attributes = new Attributes(
                reportPage,
                excelFileName,
                getReportEndDateTime(reportPage),
                getPortfolio(reportPage));
        return new ExcelAttributes(workbook, attributes);
    }

    public static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage.findByPrefix(UNIQ_TEXT, 3, 4) == TableCellAddress.NOT_FOUND) {
            throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера Промсвязьбанк");
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            String value = String.valueOf(reportPage.getNextColumnValue(PORTFOLIO_MARKER));
            return value.contains("/") ? value.split("/")[0] : value;
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
}
