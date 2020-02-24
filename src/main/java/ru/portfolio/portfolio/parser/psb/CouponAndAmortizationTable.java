package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.util.CellRangeAddress;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.EMTPY_RANGE;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class CouponAndAmortizationTable {
    private static final String TABLE_START_TEXT = "Погашение купонов и ЦБ";
    private static final String TABLE_END_TEXT = "*Налог удерживается с рублевого брокерского счета";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<Row> data = new ArrayList<>();

    public CouponAndAmortizationTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report, TABLE_START_TEXT, TABLE_END_TEXT,1));
    }

    private static List<Row> pasreTable(PsbBrokerReport report, String tableName, String tableFooterString, int leftColumn) {
        CellRangeAddress address = report.getTableCellRange(tableName, tableFooterString);
        if (address == EMTPY_RANGE) {
            return Collections.emptyList();
        }
        List<Row> data = new ArrayList<>();
        for (int rowNum = address.getFirstRow() + 2; rowNum < address.getLastRow(); rowNum++) {
            org.apache.poi.ss.usermodel.Row row = report.getSheet().getRow(rowNum);
            if (row != null) {
                Collection<Row> couponOrAmortizationOrTax = getCouponOrAmortizationOrTax(row, leftColumn);
                if (couponOrAmortizationOrTax != null) {
                    data.addAll(couponOrAmortizationOrTax);
                }
            }
        }
        return data;
    }

    private static Collection<Row> getCouponOrAmortizationOrTax(org.apache.poi.ss.usermodel.Row row, int leftColumn) {
        try {
            BigDecimal value, tax;
            boolean isCoupon = row.getCell(leftColumn + 1).getStringCellValue().equalsIgnoreCase("Погашение купона");
            double cellValue = row.getCell(leftColumn + (isCoupon ? 11 : 12)).getNumericCellValue();
            value = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue);
            cellValue = row.getCell(leftColumn + 13).getNumericCellValue();
            tax = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue).negate();
            Row.RowBuilder builder = Row.builder()
                    .timestamp(convertToInstant(row.getCell(leftColumn).getStringCellValue()))
                    .event(isCoupon ? CashFlowType.COUPON : CashFlowType.AMORTIZATION)
                    .isin(row.getCell(leftColumn + 7).getStringCellValue())
                    .count(Double.valueOf(row.getCell(leftColumn + 9).getNumericCellValue()).intValue())
                    .value(value)
                    .currency(row.getCell(leftColumn + 15).getStringCellValue());
            Collection<Row> data = new ArrayList<>();
            data.add(builder.build());
            if (!tax.equals(BigDecimal.ZERO)) {
                data.add(builder.event(CashFlowType.TAX).value(tax).build());
            }
            return data;
        } catch (Exception e) {
            log.warn("Не могу распарсить таблицу '{}' в строке {}", TABLE_START_TEXT, row.getRowNum(), e);
            return null;
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class Row {
        private String isin;
        private Instant timestamp;
        private CashFlowType event;
        private int count;
        private BigDecimal value; // НКД, амортизация или налог
        private String currency; // валюта
    }
}
