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
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.api.TableCell;
import org.spacious_team.table_wrapper.api.TableCellAddress;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;
import ru.investbook.parser.SecurityRegistrar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode(callSuper = true)
public class UralsibBrokerReport extends AbstractExcelBrokerReport {
    // "УРАЛСИБ Брокер" или "УРАЛСИБ Кэпитал - Финансовые услуги" (старый формат 2018 г)
    private static final String PORTFOLIO_MARKER = "Номер счета Клиента:";
    private final Predicate<Object> uralsibReportPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("УРАЛСИБ");
    private final Predicate<Object> dateMarkerPredicate = cell ->
            (cell instanceof String) && ((String) cell).contains("за период");
    private final Workbook book;

    public UralsibBrokerReport(ZipInputStream zis, SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
        try {
            ZipEntry zipEntry = requireNonNull(zis.getNextEntry());
            Path path = Paths.get(zipEntry.getName());
            this.book = getWorkBook(path.getFileName().toString(), zis);
            ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
            checkReportFormat(path, reportPage);
            setPath(path);
            setReportPage(reportPage);
            setPortfolio(getPortfolio(reportPage));
            setReportEndDateTime(getReportEndDateTime(reportPage));
        } catch (Exception e) {
            throw new RuntimeException("Не смог открыть excel файл", e);
        }
    }

    public UralsibBrokerReport(String excelFileName, InputStream is, SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
        this.book = getWorkBook(excelFileName, is);
        ReportPage reportPage = new ExcelSheet(book.getSheetAt(0));
        Path path = Paths.get(excelFileName);
        checkReportFormat(path, reportPage);
        setPath(path);
        setReportPage(reportPage);
        setPortfolio(getPortfolio(reportPage));
        setReportEndDateTime(getReportEndDateTime(reportPage));
    }

    private void checkReportFormat(Path path, ReportPage reportPage) {
        if (reportPage.find(0, 1, uralsibReportPredicate) == TableCellAddress.NOT_FOUND) {
            throw new RuntimeException("В файле " + path + " не содержится отчет брокера Уралсиб");
        }
    }

    private static String getPortfolio(ReportPage reportPage) {
        try {
            TableCellAddress address = reportPage.findByPrefix(PORTFOLIO_MARKER);
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
            TableCellAddress address = reportPage.find(0, dateMarkerPredicate);
            String[] words = reportPage.getCell(address)
                    .getStringValue()
                    .split(" ");
            return convertToInstant(words[words.length - 1])
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
