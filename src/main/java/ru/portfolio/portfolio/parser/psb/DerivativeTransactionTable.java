package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class DerivativeTransactionTable {
    private static final String TABLE1_START_TEXT = "Информация о заключенных сделках";
    private static final String TABLE2_START_TEXT = "Исполнение контрактов";
    private static final String TABLE_END_TEXT = "Итого";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<Row> data = new ArrayList<>();

    public DerivativeTransactionTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(parseFortsTable(report, TABLE1_START_TEXT, TABLE_END_TEXT, 1));
        this.data.addAll(parseFortsExpirationTable(report, TABLE2_START_TEXT, TABLE_END_TEXT, 1));
    }

    private static List<Row> parseFortsTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row transaction = getFortsTransaction(row, leftColumn);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    private static List<Row> parseFortsExpirationTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row transaction = getFortsExpirationTransaction(row, leftColumn);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    private static Row getFortsTransaction(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            boolean isBuy = row.getCell(leftColumn + 4).getStringCellValue().equalsIgnoreCase("покупка");
            int count = Double.valueOf(row.getCell(leftColumn + 12).getNumericCellValue()).intValue();
            String type = row.getCell(leftColumn + 2).getStringCellValue().toLowerCase();
            BigDecimal value = BigDecimal.ZERO;
            double cellValue = 0;
            switch (type) {
                case "опцион":
                    cellValue = count * row.getCell(leftColumn + 11).getNumericCellValue();
                    break;
                case "фьючерс":
                    cellValue = row.getCell(leftColumn + 13).getNumericCellValue();
                    break;
                default:
                    throw new IllegalArgumentException("Не известный контракт " + type);
            }
            if (cellValue - 0.01d > 0) {
                value = BigDecimal.valueOf(cellValue);
                if (isBuy) value = value.negate();
            }
            BigDecimal commission = BigDecimal.valueOf(row.getCell(leftColumn + 15).getNumericCellValue())
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 16).getNumericCellValue()))
                    .negate();
            return Row.builder()
                    .timestamp(convertToInstant( row.getCell(leftColumn).getStringCellValue()))
                    .transactionId(Long.parseLong(row.getCell(leftColumn + 1).getStringCellValue()))
                    .isin(row.getCell(leftColumn + 3).getStringCellValue())
                    .count(count)
                    .value(value)
                    .commission(commission)
                    .currency("RUB") // FORTS, only RUB
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в строке {}", TABLE1_START_TEXT, row.getRowNum(), e);
            return null;
        }
    }

    private static Row getFortsExpirationTransaction(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            boolean isBuy = row.getCell(leftColumn + 4).getStringCellValue().equalsIgnoreCase("покупка");
            int count = Double.valueOf(row.getCell(leftColumn + 7).getNumericCellValue()).intValue();
            String type = row.getCell(leftColumn + 2).getStringCellValue().toLowerCase();
            BigDecimal value = BigDecimal.ZERO;
            double cellValue = 0;
            if ("фьючерс".equals(type)) {
                cellValue = row.getCell(leftColumn + 8).getNumericCellValue();
            } else {
                throw new IllegalArgumentException("Не известный контракт " + type); // unexpected contract
            }
            if (cellValue - 0.01d > 0) {
                value = BigDecimal.valueOf(cellValue);
                if (isBuy) value = value.negate();
            }
            BigDecimal commission = BigDecimal.valueOf(row.getCell(leftColumn + 9).getNumericCellValue())
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 10).getNumericCellValue()))
                    .negate();
            return Row.builder()
                    .timestamp(convertToInstant( row.getCell(leftColumn).getStringCellValue()))
                    .transactionId(Long.parseLong(row.getCell(leftColumn + 1).getStringCellValue()))
                    .isin(row.getCell(leftColumn + 3).getStringCellValue())
                    .count(count)
                    .value(value)
                    .commission(commission)
                    .currency("RUB") // FORTS, only RUB
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в строке {}", TABLE2_START_TEXT, row.getRowNum(), e);
            return null;
        }
    }

    @Getter
    @Builder
    public static class Row {
        private long transactionId;
        private String isin;
        private Instant timestamp;
        private int count;
        private BigDecimal value; // оценочная стоиомсть в валюце цены
        private BigDecimal commission;
        private String currency; // валюта
    }
}
