package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class CashFlowTable {
    private static final String TABLE_START_TEXT = "Внешнее движение денежных средств в валюте счета";
    @Getter
    private final List<Row> data = new ArrayList<>();

    public CashFlowTable(PsbBrokerReport report) {
        this.data.addAll(pasreTable(report, TABLE_START_TEXT, 1));
    }

    private static List<Row> pasreTable(PsbBrokerReport report, String tableName, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum <= address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null && isDescriptionEmpty(row, leftColumn)) {
                Row cash = getCash(row, leftColumn);
                if (cash != null) {
                    data.add(cash);
                }
            }
        }
        return data;
    }

    private static boolean isDescriptionEmpty(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        Cell cell = row.getCell(leftColumn + 6);
        return cell == null ||
                cell.getCellType() == CellType.BLANK ||
                (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isEmpty());
    }

    private static Row getCash(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            String action = row.getCell(leftColumn + 4).getStringCellValue();
            boolean isPositive;
            if (action.equals("Зачислено на счет")) {
                isPositive = true;
            } else if (action.equals("Списано со счета")) {
                isPositive = false;
            } else {
                return null;
            }
            return Row.builder()
                    .timestamp(convertToInstant(row.getCell(leftColumn).getStringCellValue()))
                    .value(BigDecimal.valueOf((isPositive ? 1 : -1) * row.getCell(leftColumn + 2).getNumericCellValue()))
                    .currency(row.getCell(leftColumn + 1).getStringCellValue())
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу 'Движение денежных средств' в строке {}", row.getRowNum(), e);
            return null;
        }
    }

    @Getter
    @Builder
    public static class Row {
        private Instant timestamp;
        private BigDecimal value;
        private String currency; // валюта
    }
}
