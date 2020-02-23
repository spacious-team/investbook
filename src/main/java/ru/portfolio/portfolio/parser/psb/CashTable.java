package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;

@Slf4j
public class CashTable {
    private static final String TABLE_START_TEXT = "Позиция денежных средств по биржевым площадкам";
    private static final String TABLE_END_TEXT = "ИТОГО:";
    @Getter
    private final List<Row> data = new ArrayList<>();

    public CashTable(PsbBrokerReport report) {
        this.data.addAll(pasreTable(report, TABLE_START_TEXT, TABLE_END_TEXT, 1));
    }

    private static List<Row> pasreTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 3; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Row cash = getCash(row, leftColumn);
                if (cash != null) {
                    data.add(cash);
                }
            }
        }
        return data;
    }

    private static Row getCash(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            return Row.builder()
                    .section(row.getCell(leftColumn).getStringCellValue())
                    .value(BigDecimal.valueOf(row.getCell(leftColumn + 8).getNumericCellValue()))
                    .currency(row.getCell(leftColumn + 7).getStringCellValue())
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу 'Позиция денежных средств' в строке {}", row.getRowNum(), e);
            return null;
        }
    }

    @Getter
    @Builder
    public static class Row {
        private String section;
        private BigDecimal value;
        private String currency;
    }
}
