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

package ru.investbook.parser.uralsib;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import ru.investbook.parser.AbstractBrokerReport;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.parser.table.TableCell;
import ru.investbook.parser.table.TableCellAddress;
import ru.investbook.parser.table.excel.ExcelSheet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@EqualsAndHashCode(callSuper = true)
public class UralsibBrokerReport extends AbstractBrokerReport {
    private static final String PORTFOLIO_MARKER = "Номер счета Клиента:";
    private static final String REPORT_DATE_MARKER = "за период";
    private final Workbook book;

    public UralsibBrokerReport(ZipInputStream zis) throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();
        Path path = Paths.get(zipEntry.getName());
        this.book = getWorkBook(path.getFileName().toString(), zis);
        ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
        setPath(path);
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    public UralsibBrokerReport(String excelFileName, InputStream is) throws IOException {
        this.book = getWorkBook(excelFileName, is);
        ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(PORTFOLIO_MARKER);
            for (TableCell cell : reportPage.getRow(address.getRow())) {
                if (cell != null && cell.getColumnIndex() > address.getColumn()) {
                    Object value = cell.getValue();
                    if (value instanceof String) {
                        return value.toString()
                                .replace("_invest", "")
                                .replace("SP", "");
                    } else if (value instanceof Number) {
                        return String.valueOf(((Number) value).longValue());
                    }
                }
            }
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете", e);
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(REPORT_DATE_MARKER, 0, Integer.MAX_VALUE,
                    (cell, value) -> cell.toLowerCase().contains(value.toString()));
            return convertToInstant(
                    Lists.reverse(
                            Arrays.asList(
                                    reportPage.getRow(address.getRow())
                                            .getCell(address.getColumn())
                                            .getStringCellValue()
                                            .split(" ")))
                            .get(0))
                    .plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска даты отчета");
        }
    }

    public static String convertToCurrency(String value) {
        return value.replace("RUR", "RUB"); // uralsib uses RUR (used till 1998) code in reports
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}
