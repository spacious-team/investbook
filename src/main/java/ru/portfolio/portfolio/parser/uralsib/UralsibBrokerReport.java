/*
 * Portfolio
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

package ru.portfolio.portfolio.parser.uralsib;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import ru.portfolio.portfolio.parser.BrokerReport;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@EqualsAndHashCode(of = "path")
public class UralsibBrokerReport implements BrokerReport {
    public static final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final Workbook book;
    @Getter
    private final Sheet sheet;
    @Getter
    private final String portfolio;
    @Getter
    private final Path path;

    public UralsibBrokerReport(ZipInputStream zis) throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();
        this.path = Paths.get(zipEntry.getName());
        this.book = new HSSFWorkbook(zis); // constructor close zis
        this.sheet = book.getSheetAt(0);
        this.portfolio = getPortfolio(this.sheet);
    }

    private static String getPortfolio(Sheet sheet) {
        try {
            return sheet.getRow(7).getCell(2).getStringCellValue()
                    .replace("_invest", "")
                    .replace("SP", "");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете");
        }
    }

    @Override
    public Instant getReportDate() {
        try {
            return convertToInstant(
                    Lists.reverse(
                            Arrays.asList(
                                    sheet.getRow(2)
                                            .getCell(2)
                                            .getStringCellValue()
                                            .split(" ")))
                            .get(0));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска даты отчета");
        }
    }

    public Instant convertToInstant(String value) {
        if (value.contains(":")) {
            return LocalDateTime.parse(value, UralsibBrokerReport.dateTimeFormatter).atZone(UralsibBrokerReport.zoneId).toInstant();
        } else {
            return LocalDate.parse(value, UralsibBrokerReport.dateFormatter).atStartOfDay(UralsibBrokerReport.zoneId).toInstant();
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
