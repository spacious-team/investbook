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
public class TransactionTable {
    private static final String TABLE1_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами в дату заключения";
    private static final String TABLE2_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, рассчитанные в отчетном периоде";
    private static final String TABLE_END_TEXT = "Итого оборот";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<Row> data = new ArrayList<>();

    public TransactionTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(parseTable(report, TABLE1_START_TEXT, TABLE_END_TEXT,1));
        this.data.addAll(parseTable(report, TABLE2_START_TEXT, TABLE_END_TEXT,2));
    }

    private static List<Row> parseTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row transaction = getTransaction(row, leftColumn);
                if (transaction != null) {
                    data.add(transaction);
                }
            }
        }
        return data;
    }

    private static Row getTransaction(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            boolean isBuy = row.getCell(leftColumn + 8).getStringCellValue().equalsIgnoreCase("покупка");
            BigDecimal value, accruedInterest;
            double cellValue = row.getCell(leftColumn + 12).getNumericCellValue();
            if (cellValue - 0.01d < 0) {
                value = BigDecimal.ZERO;
            } else {
                value = BigDecimal.valueOf(cellValue);
                if (isBuy) value = value.negate();
            }
            cellValue = row.getCell(leftColumn + 13).getNumericCellValue();
            if (cellValue - 0.01d < 0) {
                accruedInterest = BigDecimal.ZERO;
            } else {
                accruedInterest = BigDecimal.valueOf(cellValue);
                if (isBuy) accruedInterest = accruedInterest.negate();
            }
            BigDecimal commission = BigDecimal.valueOf(row.getCell(leftColumn + 14).getNumericCellValue())
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 15).getNumericCellValue()))
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 16).getNumericCellValue()))
                    .add(BigDecimal.valueOf(row.getCell(leftColumn + 18).getNumericCellValue()))
                    .negate();
            return Row.builder()
                .timestamp(convertToInstant( row.getCell(leftColumn).getStringCellValue()))
                .transactionId(Long.parseLong(row.getCell(leftColumn + 1).getStringCellValue()))
                .isin(row.getCell(leftColumn + 6).getStringCellValue())
                .count(Double.valueOf(row.getCell(leftColumn + 9).getNumericCellValue()).intValue())
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(commission)
                .currency(row.getCell(leftColumn + 10).getStringCellValue().replace(" ", "").split("/")[1])
                .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу 'Сделки' в строке {}", row.getRowNum(), e);
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
        private BigDecimal accruedInterest; // НКД, в валюте бумаги
        private BigDecimal commission;
        private String currency; // валюта
    }
}
