/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.finam;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.spacious_team.table_wrapper.api.TableCellAddress.NOT_FOUND;

@EqualsAndHashCode(callSuper = true)
public class FinamBrokerReport extends AbstractExcelBrokerReport {

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private static final String UNIQ_TEXT = "Акционерное общество \u00ABИнвестиционная компания \u00ABФИНАМ\u00BB";
    private static final String PORTFOLIO_MARKER = "По счету:";
    private static final String REPORT_END_DATE_MARKER = "Справка о состоянии обязательств";
    private static final int REPORT_END_DATE_POSITION = 9;

    private final Workbook book;

    public FinamBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
        this.book = getWorkBook(excelFileName, is);
        final ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
        checkReportFormat(excelFileName, reportPage);
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    private String getPortfolio(ReportPage reportPage) {
        try {
            return String.valueOf(reportPage.getNextColumnValue(PORTFOLIO_MARKER));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + "' XXX");
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            final TableCellAddress address = reportPage.findByPrefix(REPORT_END_DATE_MARKER, 0, 1);
            @SuppressWarnings("DataFlowIssue")
            final String value = reportPage.getCell(address)
                    .getStringValue()
                    .split(" ")[REPORT_END_DATE_POSITION];
            return convertToInstant(value)
                    .plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_END_DATE_MARKER + " XXX'"
            );
        }
    }

    public static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage.findByPrefix(UNIQ_TEXT, 2) == NOT_FOUND) {
            throw new RuntimeException("В файле " + excelFileName + " не содежится отчета брокера ФИНАМ");
        }
    }

    @Override
    public void close() throws Exception {
        book.close();
    }
}
