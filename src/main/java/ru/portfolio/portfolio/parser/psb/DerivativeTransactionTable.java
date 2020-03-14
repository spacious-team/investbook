package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.FortsTableHeader.*;
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
    private final List<FortsTableRow> data = new ArrayList<>();

    public DerivativeTransactionTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(parseFortsTable(report));
        this.data.addAll(parseFortsExpirationTable(report, TABLE2_START_TEXT, TABLE_END_TEXT, 1));
    }

    private List<FortsTableRow> parseFortsTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE1_START_TEXT, TABLE_END_TEXT, FortsTableHeader.class);
        if (table.isEmpty()) {
            return Collections.emptyList();
        }
        List<FortsTableRow> data = new ArrayList<>();
        for (Row row : table) {
            if (row != null) {
                FortsTableRow transaction = getFortsTransaction(table, row);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    private List<FortsTableRow> parseFortsExpirationTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<FortsTableRow> data = new ArrayList<>();

        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                FortsTableRow transaction = getFortsExpirationTransaction(row, leftColumn);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    private FortsTableRow getFortsTransaction(ExcelTable table, Row row) {
        try {
            boolean isBuy = table.getCell(row, DIRECTION).getStringCellValue().equalsIgnoreCase("покупка");
            int count = Double.valueOf(table.getCell(row, COUNT).getNumericCellValue()).intValue();
            String type = table.getCell(row, TYPE).getStringCellValue().toLowerCase();
            BigDecimal value = BigDecimal.ZERO;
            double cellValue = 0;
            switch (type) {
                case "опцион":
                    cellValue = count * table.getCell(row, OPTION_PRICE).getNumericCellValue();
                    break;
                case "фьючерс":
                    cellValue = table.getCell(row, FUTURES_PRICE).getNumericCellValue();
                    break;
                default:
                    throw new IllegalArgumentException("Не известный контракт " + type);
            }
            if (cellValue - 0.01d > 0) {
                value = BigDecimal.valueOf(cellValue);
                if (isBuy) value = value.negate();
            }
            BigDecimal commission = BigDecimal.valueOf(table.getCell(row, MARKET_COMMISSION).getNumericCellValue())
                    .add(BigDecimal.valueOf(table.getCell(row, BROKER_COMMISSION).getNumericCellValue()))
                    .negate();
            return FortsTableRow.builder()
                    .timestamp(convertToInstant(table.getCell(row, DATE_TIME).getStringCellValue()))
                    .transactionId(Long.parseLong(table.getCell(row, TRANSACTION).getStringCellValue()))
                    .isin(table.getCell(row, CONTRACT).getStringCellValue())
                    .count((isBuy ? 1 : -1) * count)
                    .value(value)
                    .commission(commission)
                    .currency("RUB") // FORTS, only RUB
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", TABLE1_START_TEXT, report.getPath(), row.getRowNum(), e);
            return null;
        }
    }

    private FortsTableRow getFortsExpirationTransaction(Row row, int leftColumn) {
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
            return FortsTableRow.builder()
                    .timestamp(convertToInstant( row.getCell(leftColumn).getStringCellValue()))
                    .transactionId(Long.parseLong(row.getCell(leftColumn + 1).getStringCellValue()))
                    .isin(row.getCell(leftColumn + 3).getStringCellValue())
                    .count(count)
                    .value(value)
                    .commission(commission)
                    .currency("RUB") // FORTS, only RUB
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", TABLE2_START_TEXT, report.getPath(), row.getRowNum(), e);
            return null;
        }
    }

    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("№"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        FUTURES_PRICE("сумма срочной сделки"),
        OPTION_PRICE("цена опциона"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;

        FortsTableHeader(String ... words) {
            this.column = TableColumn.of(words);
        }
    }

    @Getter
    @Builder
    static class FortsTableRow {
        private long transactionId;
        private String isin;
        private Instant timestamp;
        private int count;
        private BigDecimal value; // оценочная стоиомсть в валюце цены
        private BigDecimal commission;
        private String currency; // валюта

    }
}
