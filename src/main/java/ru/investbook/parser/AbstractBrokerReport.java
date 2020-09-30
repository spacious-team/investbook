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

package ru.investbook.parser;

import lombok.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.investbook.parser.table.ReportPage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@EqualsAndHashCode(of = "path")
public abstract class AbstractBrokerReport implements BrokerReport {

    protected static final int LAST_TRADE_HOUR = 19;
    @Setter(AccessLevel.PROTECTED)
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    @Setter(AccessLevel.PROTECTED)
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Path path;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String portfolio;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private ReportPage reportPage;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Instant reportEndDateTime;
    @Getter
    private final ZoneId reportZoneId = ZoneId.of("Europe/Moscow");

    protected Workbook getWorkBook(String excelFileName, InputStream is) throws IOException {
        if (excelFileName.endsWith(".xls")) {
            return new HSSFWorkbook(is); // constructor close zis
        } else {
            return new XSSFWorkbook(is);
        }
    }

    public Instant convertToInstant(String value) {
        value = value.trim();
        if (value.contains(":")) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(getReportZoneId()).toInstant();
        } else {
            return LocalDate.parse(value, dateFormatter).atStartOfDay(getReportZoneId()).toInstant();
        }
    }
}
