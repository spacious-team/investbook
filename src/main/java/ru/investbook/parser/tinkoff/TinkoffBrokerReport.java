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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static ru.investbook.parser.tinkoff.TinkoffBrokerReportHelper.removePageNumRows;

@EqualsAndHashCode(callSuper = true)
public class TinkoffBrokerReport extends AbstractExcelBrokerReport {
    static final Pattern tablesLastRowPattern = Pattern.compile("^[0-9]+\\.[0-9]+\\s+\\b", UNICODE_CHARACTER_CLASS);
    private static final String PORTFOLIO_MARKER = "Инвестор:";
    private static final Predicate<Object> tinkoffReportPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("Тинькофф");
    private static final Predicate<Object> dateMarkerPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("за период");

    private final Workbook book;

    public TinkoffBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
        this.book = getWorkBook(excelFileName, is);
        ExcelSheet reportPage = new ExcelSheet(book.getSheetAt(0));
        checkReportFormat(excelFileName, reportPage);
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
        removePageNumRows(reportPage);
    }

    public static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage.find(0, 2, tinkoffReportPredicate) == TableCellAddress.NOT_FOUND) {
            throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера Тинькофф");
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            return reportPage.getCell(reportPage.findByPrefix(PORTFOLIO_MARKER))
                    .getStringValue()
                    .split("/")[1]
                    .trim()
                    .split("\s+")[0];
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER);
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(0, 10, dateMarkerPredicate);
            String[] words = reportPage.getCell(address)
                    .getStringValue()
                    .split("-");
            String value = words[words.length - 1].trim();
            return convertToInstant(value).plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException("Не найдена дата отчета по заданному шаблону");
        }
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}
