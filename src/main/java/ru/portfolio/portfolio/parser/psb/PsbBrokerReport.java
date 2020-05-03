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

package ru.portfolio.portfolio.parser.psb;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.portfolio.portfolio.parser.BrokerReport;
import ru.portfolio.portfolio.parser.ExcelTableHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode(of = "path")
public class PsbBrokerReport implements BrokerReport {
    private static final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final String PORTFOLIO_MARKER = "Договор №:";
    private static final String REPORT_DATE_MARKER = "ОТЧЕТ БРОКЕРА";

    private final Workbook book;
    @Getter
    private final Sheet sheet;
    @Getter
    private final String portfolio;
    @Getter
    private final Path path;
    @Getter
    private final Instant reportDate;

    public PsbBrokerReport(String excelFileName) throws IOException {
        this(Paths.get(excelFileName));
    }

    public PsbBrokerReport(Path report) throws IOException {
        this(report.getFileName().toString(), Files.newInputStream(report));
    }

    public PsbBrokerReport(String excelFileName, InputStream is) throws IOException {
        this.path = Paths.get(excelFileName);
        this.book = getWorkBook(excelFileName, is);
        this.sheet = book.getSheetAt(0);
        this.portfolio = getPortfolio(this.sheet);
        this.reportDate = getReportDate(this.sheet);
    }

    private Workbook getWorkBook(String excelFileName, InputStream is) throws IOException {
        if (excelFileName.endsWith(".xls")) {
            return new HSSFWorkbook(is); // constructor close zis
        } else {
            return new XSSFWorkbook(is);
        }
    }

    private static String getPortfolio(Sheet sheet) {
        try {
            CellAddress address = ExcelTableHelper.find(sheet, PORTFOLIO_MARKER);
            for (Cell cell : sheet.getRow(address.getRow())) {
                if (cell != null && cell.getColumnIndex() > address.getColumn() && cell.getCellType() == CellType.STRING) {
                    String value = ExcelTableHelper.getStringCellValue(cell);
                    return value.contains("/") ? value.split("/")[0] : value;
                }
            }
            throw new IllegalArgumentException(
                    "В отчете не найден номер договора по заданному шаблону '" + PORTFOLIO_MARKER + " XXX'");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете");
        }
    }

    private Instant getReportDate(Sheet sheet) {
        try {

            CellAddress address = ExcelTableHelper.find(sheet, REPORT_DATE_MARKER);
            for (Cell cell : sheet.getRow(address.getRow())) {
                if (cell != null && cell.getColumnIndex() > address.getColumn() && cell.getCellType() == CellType.STRING) {
                    return convertToInstant(ExcelTableHelper.getStringCellValue(cell).split(" ")[3]);
                }
            }
            throw new IllegalArgumentException(
                    "Не найдена дата отчета по заданному шаблону '" + REPORT_DATE_MARKER + " XXX'");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска даты отчета");
        }
    }

    public Instant convertToInstant(String value) {
        if (value.contains(":")) {
            return LocalDateTime.parse(value, PsbBrokerReport.dateTimeFormatter).atZone(PsbBrokerReport.zoneId).toInstant();
        } else {
            return LocalDate.parse(value, PsbBrokerReport.dateFormatter).atStartOfDay(PsbBrokerReport.zoneId).toInstant();
        }
    }

    @Override
    public void close() throws IOException {
        this.book.close();
    }
}
