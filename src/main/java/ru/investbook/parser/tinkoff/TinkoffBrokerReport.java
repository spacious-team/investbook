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

package ru.investbook.parser.tinkoff;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static ru.investbook.parser.tinkoff.TinkoffBrokerReportHelper.removePageNumRows;

@EqualsAndHashCode(callSuper = true)
public class TinkoffBrokerReport extends AbstractExcelBrokerReport {
    static final Pattern tablesLastRowPattern = Pattern.compile("^[0-9]+\\.[0-9]+\\s+\\b", UNICODE_CHARACTER_CLASS);
    private static final String PORTFOLIO_MARKER = "Инвестор:";
    private static final Predicate<Object> tinkoffReportPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("Тинькофф");
    private static final Predicate<Object> tbankReportPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("ТБанк");
    private static final Predicate<Object> dateMarkerPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("за период");

    public TinkoffBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(getBrokerReportAttributes(excelFileName, is), securityRegistrar);
    }

    private static ExcelAttributes getBrokerReportAttributes(String excelFileName, InputStream is) {
        Workbook workbook = getWorkBook(excelFileName, is);
        ExcelSheet reportPage = new ExcelSheet(workbook.getSheetAt(0));
        checkReportFormat(excelFileName, reportPage);
        removePageNumRows(reportPage);
        Attributes attributes = new Attributes(
                reportPage,
                excelFileName,
                getReportEndDateTime(reportPage),
                getPortfolio(reportPage));
        return new ExcelAttributes(workbook, attributes);
    }

    public static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage.find(0, 2, tbankReportPredicate) == TableCellAddress.NOT_FOUND &&
                reportPage.find(0, 2, tinkoffReportPredicate) == TableCellAddress.NOT_FOUND) {
            throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера ТБанк (Тинькофф)");
        }
    }

    @SuppressWarnings({"nullness", "DataFlowIssue"})
    private static String getPortfolio(ReportPage reportPage) {
        try {
            return reportPage.getCell(reportPage.findByPrefix(PORTFOLIO_MARKER))
                    .getStringValue()
                    .split("/")[1]
                    .trim()
                    .split("\\s+")[0];
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER);
        }
    }

    private static Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(0, 10, dateMarkerPredicate);
            @SuppressWarnings({"nullness", "DataFlowIssue"})
            String[] words = reportPage.getCell(address)
                    .getStringValue()
                    .split("-");
            String value = words[words.length - 1].trim();
            return convertToInstantWithRussianFormatAndMoscowZoneId(value)
                    .plus(LAST_TRADE_HOUR, HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException("Не найдена дата отчета по заданному шаблону");
        }
    }
}
