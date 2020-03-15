package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.FortsTableHeader.*;
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
        this.data.addAll(parseFortsExpirationTable(report));
    }

    private List<FortsTableRow> parseFortsTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE1_START_TEXT, TABLE_END_TEXT, FortsTableHeader.class);
        return table.getDataCollection(report.getPath(), DerivativeTransactionTable::getFortsTransaction);
    }

    private List<FortsTableRow> parseFortsExpirationTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE2_START_TEXT, TABLE_END_TEXT, ExpirationTableHeader.class);
        return table.getDataCollection(report.getPath(), DerivativeTransactionTable::getFortsExpirationTransaction);
    }

    private static Collection<FortsTableRow> getFortsTransaction(ExcelTable table, Row row) {
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
                cellValue = table.getCell(row, VALUE).getNumericCellValue();
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
        return singletonList(FortsTableRow.builder()
                .timestamp(convertToInstant(table.getCell(row, DATE_TIME).getStringCellValue()))
                .transactionId(Long.parseLong(table.getCell(row, TRANSACTION).getStringCellValue()))
                .isin(table.getCell(row, CONTRACT).getStringCellValue())
                .count((isBuy ? 1 : -1) * count)
                .value(value)
                .commission(commission)
                .currency("RUB") // FORTS, only RUB
                .build());
    }

    private static Collection<FortsTableRow> getFortsExpirationTransaction(ExcelTable table, Row row) {
        boolean isBuy = table.getCell(row, ExpirationTableHeader.DIRECTION).getStringCellValue().equalsIgnoreCase("покупка");
        int count = Double.valueOf(table.getCell(row, ExpirationTableHeader.COUNT).getNumericCellValue()).intValue();
        String type = table.getCell(row, ExpirationTableHeader.TYPE).getStringCellValue().toLowerCase();
        BigDecimal value = BigDecimal.ZERO;
        double cellValue = 0;
        if ("фьючерс".equals(type)) {
            cellValue = table.getCell(row, ExpirationTableHeader.VALUE).getNumericCellValue();
        } else {
            throw new IllegalArgumentException("Не известный контракт '" + type + "'"); // unexpected contract
        }
        if (cellValue - 0.01d > 0) {
            value = BigDecimal.valueOf(cellValue);
            if (isBuy) value = value.negate();
        }
        BigDecimal commission = BigDecimal.valueOf(table.getCell(row, ExpirationTableHeader.MARKET_COMMISSION).getNumericCellValue())
                .add(BigDecimal.valueOf(table.getCell(row, ExpirationTableHeader.BROKER_COMMISSION).getNumericCellValue()))
                .negate();
        return singletonList(FortsTableRow.builder()
                .timestamp(convertToInstant(table.getCell(row, ExpirationTableHeader.DATE_TIME).getStringCellValue()))
                .transactionId(Long.parseLong(table.getCell(row, ExpirationTableHeader.TRANSACTION).getStringCellValue()))
                .isin(table.getCell(row, ExpirationTableHeader.CONTRACT).getStringCellValue())
                .count(count)
                .value(value)
                .commission(commission)
                .currency("RUB") // FORTS, only RUB
                .build());
    }

    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("№"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма срочной сделки"),
        OPTION_PRICE("цена опциона"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = TableColumn.of(words);
        }
    }

    enum ExpirationTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        ExpirationTableHeader(String ... words) {
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
