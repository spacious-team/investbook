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

package ru.investbook.parser.vtb;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import ru.investbook.parser.AbstractBrokerReport;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.TableCellAddress;
import ru.investbook.parser.table.excel.ExcelSheet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@EqualsAndHashCode(callSuper = true)
public class VtbBrokerReport extends AbstractBrokerReport {
    private static final String UNIQ_TEXT = VtbBrokerReport.REPORT_DATE_MARKER;
    private static final String PORTFOLIO_MARKER = "№ субсчета:";
    private static final String REPORT_DATE_MARKER = "Отчет Банка ВТБ";

    private final Workbook book;

    public VtbBrokerReport(String excelFileName, InputStream is) {
        this.book = getWorkBook(excelFileName, is);
        ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
        Path path = Paths.get(excelFileName);
        checkReportFormat(path, reportPage);
        setPath(path);
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    private static void checkReportFormat(Path path, ReportPage reportPage) {
        if (reportPage.find(UNIQ_TEXT, 1, 2) == TableCellAddress.NOT_FOUND) {
            throw new RuntimeException("В файле " + path + " не содержится отчет брокера ВТБ");
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            Object value = reportPage.getNextColumnValue(PORTFOLIO_MARKER);
            if (value instanceof Number) {
                value = ((Number) value).intValue();
            }
            return String.valueOf(value);
        } catch (Exception e) {
            throw new RuntimeException("В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(REPORT_DATE_MARKER, 1, 2);
            String value = reportPage.getCell(address)
                    .getStringCellValue()
                    .split(" ")[9];
            return convertToInstant(value)
                    .plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        }
    }

    public static String convertToCurrency(String value) {
        return value.replace("RUR", "RUB"); // vtb uses RUR (used till 1998) code in reports
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}
