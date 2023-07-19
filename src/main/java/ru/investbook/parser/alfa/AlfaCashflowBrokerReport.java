/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.alfa;

import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
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
import java.time.temporal.ChronoUnit;

@EqualsAndHashCode(callSuper = true)
public class AlfaCashflowBrokerReport extends AbstractExcelBrokerReport {
    public static final String UNIQ_TEXT = "Движение денежных средств за период";
    private static final String PORTFOLIO_MARKER = "Ген. соглашение";
    private static final String REPORT_DATE_MARKER = "Отчетный период";

    private final Workbook book;

    public AlfaCashflowBrokerReport(String excelFileName, SecurityRegistrar securityRegistrar) throws IOException {
        this(Paths.get(excelFileName), securityRegistrar);
    }

    public AlfaCashflowBrokerReport(Path report, SecurityRegistrar securityRegistrar) throws IOException {
        this(report.getFileName().toString(), Files.newInputStream(report), securityRegistrar);
    }

    public AlfaCashflowBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
        this.book = getWorkBook(excelFileName, is);
        ReportPage reportPage = new ExcelSheet(book.getSheetAt(3));
        checkReportFormat(excelFileName, reportPage);
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    public static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage instanceof ExcelSheet excelSheet) {

            if (!excelSheet.getSheet().getSheetName().trim().equals("Движение ДС")) {
                throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера Альфа-Инвестиции");
            }

            if (reportPage.findByPrefix(UNIQ_TEXT, 11, 13) == TableCellAddress.NOT_FOUND) {
                throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера Альфа-Инвестиции");
            }
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            String value = String.valueOf(reportPage.getNextColumnValue(PORTFOLIO_MARKER));

            return StringUtils.substringBetween(value, "№ ", " от");

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        }
    }

    private Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            String value = String.valueOf(reportPage.getNextColumnValue(REPORT_DATE_MARKER));
            return convertToInstant(value.split(" - ")[1].trim())
                    .plus(LAST_TRADE_HOUR, ChronoUnit.HOURS);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        }
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}
