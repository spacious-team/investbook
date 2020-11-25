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

package ru.investbook.parser.vtb;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@EqualsAndHashCode(callSuper = true)
public class VtbBrokerReport extends AbstractExcelBrokerReport {
    private static final String UNIQ_TEXT = VtbBrokerReport.REPORT_DATE_MARKER;
    private static final String ACCOUNT_MARKER = "№ и дата Соглашения";
    private static final String SUBACCOUNT_MARKER = "№ субсчета:";
    private static final String REPORT_DATE_MARKER = "Отчет Банка ВТБ";
    static final BigDecimal minValue = BigDecimal.valueOf(0.01);

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
            Object account = reportPage.getNextColumnValue(ACCOUNT_MARKER);
            Object subAccount = reportPage.getNextColumnValue(SUBACCOUNT_MARKER);
            if (account instanceof Number) account = ((Number) account).intValue();
            if (subAccount instanceof Number) subAccount = ((Number) subAccount).intValue();
            if (account.equals(subAccount)) {
                return String.valueOf(account);
            } else {
                return account + "-" + subAccount;
            }
        } catch (Exception e) {
            throw new RuntimeException("В отчете не найден номер договора по заданному шаблону '" + SUBACCOUNT_MARKER + " XXX'");
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
