package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.NOT_ADDRESS;

@Slf4j
public class TransactionTable {
    private static final String TABLE1_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами в дату заключения";
    private static final String TABLE2_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, рассчитанные в отчетном периоде";
    private static final String TABLE_END_TEXT = "Итого оборот";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final ZoneId zoneId = ZoneId.of("Europe/Moscow");
    @Getter
    private final List<Row> data = new ArrayList<>();

    public TransactionTable(PsbBrokerReport report) {
        this.data.addAll(pasreTable1(report, TABLE1_START_TEXT, 1));
        this.data.addAll(pasreTable1(report, TABLE2_START_TEXT, 2));
    }

    private static CellRangeAddress getPortfolioTableAddress(PsbBrokerReport report, String tableName) {
        CellAddress startAddress = report.find(tableName);
        if (startAddress.equals(NOT_ADDRESS)) {
            return EMTPY_RANGE;
        }
        CellAddress endAddress = report.find(TABLE_END_TEXT, startAddress.getRow() + 2,
                report.getSheet().getLastRowNum(), (cell , prefix) -> cell.startsWith(prefix.toString()));
        if (endAddress.equals(NOT_ADDRESS)) {
            return EMTPY_RANGE;
        }
        return new CellRangeAddress(
                startAddress.getRow(),
                endAddress.getRow(),
                report.getSheet().getRow(startAddress.getRow()).getFirstCellNum(),
                report.getSheet().getRow(endAddress.getRow()).getLastCellNum());
    }

    private static Row cast(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            boolean isBuy = row.getCell(leftColumn + 8).getStringCellValue().equals("покупка");
            BigDecimal commission = BigDecimal.valueOf(row.getCell(leftColumn + 14).getNumericCellValue())
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 15).getNumericCellValue()))
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 16).getNumericCellValue()))
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 18).getNumericCellValue()));
        return Row.builder()
                .timestamp(getLocalDateTime( row.getCell(leftColumn).getStringCellValue()).toInstant())
                .isin(row.getCell(leftColumn + 6).getStringCellValue())
                .count((isBuy ? 1 : -1) * Double.valueOf(row.getCell(leftColumn + 9).getNumericCellValue()).intValue())
                .amount(BigDecimal.valueOf(row.getCell(leftColumn + 12).getNumericCellValue()))
                .accruedInterest(BigDecimal.valueOf(row.getCell(leftColumn + 13).getNumericCellValue()))
                .commission(commission)
                .currency(row.getCell(leftColumn + 10).getStringCellValue().replace(" ", "").split("/")[1])
                .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу 'Сделки' в строке {}", row.getRowNum(), e);
            return null;
        }
    }

    private static ZonedDateTime getLocalDateTime(String value) {
        if (value.contains(":")) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(zoneId);
        } else {
            return LocalDate.parse(value, dateFormatter).atStartOfDay(zoneId);
        }
    }

    private List<Row> pasreTable1(PsbBrokerReport report, String tableName, int leftColumn) {
        CellRangeAddress address = getPortfolioTableAddress(report, tableName);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row transaction = cast(row, leftColumn);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    @Getter
    @Builder
    public static class Row {
        private String isin;
        private Instant timestamp;
        private int count;
        private BigDecimal amount; // оценочная стоиомсть в валюце цены
        private BigDecimal accruedInterest; // НКД, в валюте бумаги
        private BigDecimal commission;
        private String currency; // валюта
    }
}
