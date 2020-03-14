package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.CouponAndAmortizationTable.CouponAndAmortizationTableHeader.*;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class CouponAndAmortizationTable {
    private static final String TABLE_START_TEXT = "Погашение купонов и ЦБ";
    private static final String TABLE_END_TEXT = "*Налог удерживается с рублевого брокерского счета";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<CouponAndAmortizationTableRow> data = new ArrayList<>();

    public CouponAndAmortizationTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report));
    }

    private List<CouponAndAmortizationTableRow> pasreTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE_START_TEXT, TABLE_END_TEXT, CouponAndAmortizationTableHeader.class);
        return table.isEmpty() ?
                Collections.emptyList() :
                table.getDataCollection(report.getPath(), CouponAndAmortizationTable::getCouponOrAmortizationOrTax);
    }

    private static Collection<CouponAndAmortizationTableRow> getCouponOrAmortizationOrTax(ExcelTable table, Row row) {
        BigDecimal value, tax;
        CashFlowType event;
        String action = table.getCell(row, TYPE).getStringCellValue();
        if (action.equalsIgnoreCase("Погашение купона")) {
            event = CashFlowType.COUPON;
        } else if (action.equalsIgnoreCase("Амортизация")) {
            event = CashFlowType.AMORTIZATION;
        } else if (action.equalsIgnoreCase("Погашение бумаг")) {
            event = CashFlowType.REDEMPTION;
        } else {
            throw new RuntimeException("Обработчик события " + action + " не реализован");
        }

        double cellValue = ((event == CashFlowType.COUPON) ?
                table.getCell(row, COUPON) :
                table.getCell(row, VALUE))
                .getNumericCellValue();
        value = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue);
        cellValue = table.getCell(row, TAX).getNumericCellValue();
        tax = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue).negate();
        CouponAndAmortizationTableRow.CouponAndAmortizationTableRowBuilder builder = CouponAndAmortizationTableRow.builder()
                .timestamp(convertToInstant(table.getCell(row, DATE).getStringCellValue()))
                .event(event)
                .isin(table.getCell(row, ISIN).getStringCellValue())
                .count(Double.valueOf(table.getCell(row, COUNT).getNumericCellValue()).intValue())
                .value(value)
                .currency(table.getCell(row, CURRENCY).getStringCellValue());
        Collection<CouponAndAmortizationTableRow> data = new ArrayList<>();
        data.add(builder.build());
        if (!tax.equals(BigDecimal.ZERO)) {
            data.add(builder.event(CashFlowType.TAX).value(tax).build());
        }
        return data;
    }

    enum CouponAndAmortizationTableHeader implements TableColumnDescription {
        DATE("дата"),
        TYPE("вид операции"),
        ISIN("isin"),
        COUNT("кол-во"),
        COUPON("нкд"),
        VALUE("сумма амортизации"),
        TAX("удержанного налога"),
        CURRENCY("валюта выплаты");

        @Getter
        private final TableColumn column;

        CouponAndAmortizationTableHeader(String... words) {
            this.column = TableColumn.of(words);
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class CouponAndAmortizationTableRow {
        private String isin;
        private Instant timestamp;
        private CashFlowType event;
        private int count;
        private BigDecimal value; // НКД, амортизация или налог
        private String currency; // валюта
    }
}
