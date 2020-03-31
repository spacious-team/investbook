package ru.portfolio.portfolio.parser.psb;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.portfolio.portfolio.parser.BrokerReport;

import java.io.IOException;
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

    private final Workbook book;
    @Getter
    private final Sheet sheet;
    @Getter
    private final String portfolio;
    @Getter
    private final Path path;

    public PsbBrokerReport(String exelFileName) throws IOException {
        this(Paths.get(exelFileName));
    }

    public PsbBrokerReport(Path exelFileName) throws IOException {
        this.path = exelFileName;
        this.book = new XSSFWorkbook(Files.newInputStream(exelFileName));
        this.sheet = book.getSheetAt(0);
        this.portfolio = getPortfolio(this.sheet);
    }

    private static String getPortfolio(Sheet sheet) {
        try {
            return sheet.getRow(9).getCell(3).getStringCellValue().split("/")[0];
        } catch (Exception e) {
            throw new RuntimeException("Ошибка поиска номера Брокерского счета в отчете");
        }
    }

    @Override
    public Instant getReportDate() {
        try {
            return convertToInstant(sheet.getRow(6).getCell(3).getStringCellValue().split(" ")[3]);
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
