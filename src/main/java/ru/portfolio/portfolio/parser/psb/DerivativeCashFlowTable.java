package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;
import ru.portfolio.portfolio.pojo.CashFlowEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class DerivativeCashFlowTable {
    private static final String TABLE1_START_TEXT = "Движение стандартных контрактов";
    private static final String TABLE2_START_TEXT = "Прочие операции";
    private static final String TABLE_END_TEXT = "Итого";
    @Getter
    private final List<Row> data = new ArrayList<>();

    public DerivativeCashFlowTable(PsbBrokerReport report) {
        Map<String, Integer> contractCount = pasreDerivativeCountTable(report, TABLE1_START_TEXT, TABLE_END_TEXT, 1);
        if (!contractCount.isEmpty()) {
            this.data.addAll(pasreDerivativeCashFlowTable(report, TABLE2_START_TEXT, TABLE_END_TEXT, 1, contractCount));
        }
    }

    private static Map<String, Integer> pasreDerivativeCountTable(PsbBrokerReport report, String tableName,
                                                                  String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyMap();
        }
        Map<String, Integer> contractCount = new HashMap<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                String contract = row.getCell(leftColumn + 1).getStringCellValue();
                int incomingCount = Math.abs(getIntCellValue(row, leftColumn + 2));
                int outgoingCount = Math.abs(getIntCellValue(row, leftColumn + 5));
                int count = Math.max(incomingCount, outgoingCount);
                if (count == 0) {
                    count = Math.abs(getIntCellValue(row, leftColumn + 3)); // buyCount == cellCount
                }
                if (count != 0) {
                    contractCount.put(contract, count);
                }
            }
        }
        return contractCount;
    }

    private static List<Row> pasreDerivativeCashFlowTable(PsbBrokerReport report, String tableName,
                                                          String tableFooterString, int leftColumn,
                                                          Map<String, Integer> contractCount) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row cash = getDerivativeCashFlow(row, leftColumn, contractCount);
                if (cash != null) {
                    data.add(cash);
                }
            }
        }
        return data;
    }

    private static Row getDerivativeCashFlow(org.apache.poi.ss.usermodel.Row row, int leftColumn, Map<String, Integer> contractCount) {
        try {
            BigDecimal value = BigDecimal.valueOf(row.getCell(leftColumn + 6).getNumericCellValue())
                    .subtract(BigDecimal.valueOf(row.getCell(leftColumn + 5).getNumericCellValue()));
            Row.RowBuilder builder = Row.builder()
                    .timestamp(convertToInstant(row.getCell(leftColumn).getStringCellValue()))
                    .value(value)
                    .currency("RUB"); // FORTS, only RUB
            String action = row.getCell(leftColumn + 2).getStringCellValue().toLowerCase();
            switch (action) {
                case "вариационная маржа":
                    String contract = row.getCell(leftColumn + 1).getStringCellValue().split("/")[1].trim();
                    Integer count = contractCount.get(contract);
                    if (count == null) {
                        throw new IllegalArgumentException("Количество открытых контрактов не найдено");
                    }
                    return builder.event(CashFlowEvent.DERIVATIVE_PROFIT)
                            .contract(contract)
                            .count(count)
                            .build();
                case "биржевой сбор":
                    return builder.event(CashFlowEvent.COMMISSION).build();
                case "заблокированo / Разблокировано средств под ГО":
                    return null; // не влияет на размер собственных денежных средств
                default:
                    throw new IllegalArgumentException("Неизвестный вид операции " + action);
            }
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу деривативов '{}' в строке {}", TABLE2_START_TEXT, row.getRowNum(), e);
            return null;
        }
    }

    private static int getIntCellValue(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        return Double.valueOf(row.getCell(leftColumn).getNumericCellValue()).intValue();
    }

    @Getter
    @Builder
    public static class Row {
        private String contract;
        private Instant timestamp;
        private CashFlowEvent event;
        private Integer count;
        private BigDecimal value;
        private String currency; // валюта
    }
}
