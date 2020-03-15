package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.AbstractReportTable;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import static ru.portfolio.portfolio.parser.psb.CouponAndAmortizationTable.CouponAndAmortizationTableHeader.*;

@Slf4j
public class CouponAndAmortizationTable extends AbstractReportTable<CouponAndAmortizationTable.CouponAndAmortizationTableRow> {

    private static final String TABLE_NAME = "Погашение купонов и ЦБ";
    private static final String TABLE_END_TEXT = "*Налог удерживается с рублевого брокерского счета";

    public CouponAndAmortizationTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, CouponAndAmortizationTableHeader.class);
    }

    @Override
    protected Collection<CouponAndAmortizationTableRow> getRow(ExcelTable table, Row row) {
        CashFlowType event;
        String action = table.getStringCellValue(row, TYPE);
        if (action.equalsIgnoreCase("Погашение купона")) {
            event = CashFlowType.COUPON;
        } else if (action.equalsIgnoreCase("Амортизация")) {
            event = CashFlowType.AMORTIZATION;
        } else if (action.equalsIgnoreCase("Погашение бумаг")) {
            event = CashFlowType.REDEMPTION;
        } else {
            throw new RuntimeException("Обработчик события " + action + " не реализован");
        }

        BigDecimal value = ((event == CashFlowType.COUPON) ?
                table.getCurrencyCellValue(row, COUPON) :
                table.getCurrencyCellValue(row, VALUE));
        BigDecimal tax = table.getCurrencyCellValue(row, TAX).negate();
        CouponAndAmortizationTableRow.CouponAndAmortizationTableRowBuilder builder = CouponAndAmortizationTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .event(event)
                .isin(table.getStringCellValue(row, ISIN))
                .count(table.getIntCellValue(row, COUNT))
                .value(value)
                .currency(table.getStringCellValue(row, CURRENCY));
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
