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
public class PortfolioSecuritiesTable {
    private static final String TABLE_START_TEXT = "Портфель на конец дня на биржевом рынке";
    private static final String INVALID_TEXT = "Итого в валюте цены";
    private static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<Row> data = new ArrayList<>();

    public PortfolioSecuritiesTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report));
    }

    private List<Row> pasreTable(PsbBrokerReport report) {
        CellRangeAddress address =  report.getTableCellRange(TABLE_START_TEXT, TABLE_END_TEXT);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null && !report.rowContains(rowNum, INVALID_TEXT)) {
                Row position = getPosition(row);
                if (position != null) {
                    data.add(position);
                }
            }
        }
        return data;
    }

    private Row getPosition(org.apache.poi.ss.usermodel.Row row) {
        try {
            return Row.builder()
                    .name(row.getCell(1).getStringCellValue())
                    .isin(row.getCell(4).getStringCellValue())
                    .buyCount(Double.valueOf(row.getCell(7).getNumericCellValue()).intValue())
                    .cellCount(Double.valueOf(row.getCell(8).getNumericCellValue()).intValue())
                    .outgoingCount(Double.valueOf(row.getCell(9).getNumericCellValue()).intValue())
                    .currency(row.getCell(11).getStringCellValue())
                    .amount(BigDecimal.valueOf(row.getCell(13).getNumericCellValue()))
                    .accruedInterest(BigDecimal.valueOf(row.getCell(14).getNumericCellValue()))
                    .build();
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в файле {}, строка {}", TABLE_START_TEXT, report.getPath(), row.getRowNum(), e);
            return null;
        }
    }

    @Getter
    @Builder
    public static class Row {
        private String isin;
        private String name;
        private int buyCount;
        private int cellCount;
        private int outgoingCount;
        private BigDecimal amount; // оценочная стоиомсть в валюце цены
        private BigDecimal accruedInterest; // НКД, в валюте бумаги
        private String currency; // валюта
    }
}
