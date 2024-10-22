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

package ru.investbook.parser.uralsib;

import lombok.EqualsAndHashCode;
import org.apache.poi.ss.usermodel.Workbook;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCell;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;
import ru.investbook.parser.SecurityRegistrar;

import java.io.InputStream;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.spacious_team.table_wrapper.api.TableCellAddress.NOT_FOUND;

@EqualsAndHashCode(callSuper = true)
public class UralsibBrokerReport extends AbstractExcelBrokerReport {
    // "УРАЛСИБ Брокер" или "УРАЛСИБ Кэпитал - Финансовые услуги" (старый формат 2018 г)
    private static final String PORTFOLIO_MARKER = "Номер счета Клиента:";
    private static final Predicate<Object> uralsibReportPredicate = cell ->
            (cell instanceof String value) && (value.contains("Твой Брокер") || value.contains("УРАЛСИБ"));
    private static final Predicate<Object> dateMarkerPredicate = cell ->
            (cell instanceof String value) && value.contains("за период");

    public UralsibBrokerReport(ZipInputStream zis, SecurityRegistrar securityRegistrar) {
        this(getFileName(zis), zis, securityRegistrar);
    }

    public UralsibBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(getBrokerReportAttributes(excelFileName, is), securityRegistrar);
    }

    @SuppressWarnings("DataFlowIssue")
    private static String getFileName(ZipInputStream zis) {
        try {
            return zis.getNextEntry()
                    .getName();
        } catch (Exception e) {
            throw new RuntimeException("Не смог открыть excel файл", e);
        }
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

    private static void checkReportFormat(String excelFileName, ReportPage reportPage) {
        if (reportPage.find(0, 1, uralsibReportPredicate) == NOT_FOUND) {
            throw new RuntimeException("В файле " + excelFileName + " не содержится отчет брокера Твой Брокер (Уралсиб)");
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.findByPrefix(PORTFOLIO_MARKER);
            //noinspection DataFlowIssue
            for (@Nullable TableCell cell : reportPage.getRow(address.getRow())) {
                if (cell != null && cell.getColumnIndex() > address.getColumn()) {
                    @SuppressWarnings("DataFlowIssue")
                    @Nullable Object value = cell.getValue();
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

    private static Instant getReportEndDateTime(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.find(0, dateMarkerPredicate);
            @SuppressWarnings({"nullness", "DataFlowIssue"})
            String[] words = reportPage.getCell(address)
                    .getStringValue()
                    .split(" ");
            String date = words[words.length - 1];
            return convertToInstantWithRussianFormatAndMoscowZoneId(date)
                    .plus(LAST_TRADE_HOUR, HOURS);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска даты отчета");
        }
    }

    public static String convertToCurrency(String value) {
        return value.replace("RUR", "RUB"); // uralsib uses RUR (used till 1998) code in reports
    }
}
