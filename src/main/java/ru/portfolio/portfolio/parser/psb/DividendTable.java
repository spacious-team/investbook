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
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.DividendTable.DividendTableHeader.*;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class DividendTable {
    private static final String TABLE_START_TEXT = "Выплата дивидендов";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<DividendTableRow> data = new ArrayList<>();

    public DividendTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report));
    }

    private List<DividendTableRow> pasreTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE_START_TEXT, DividendTableHeader.class);
        return table.getDataCollection(report.getPath(), DividendTable::getDividendAndTax);
    }

    private static Collection<DividendTableRow> getDividendAndTax(ExcelTable table, Row row) {
        BigDecimal value, tax;
        double cellValue = table.getCell(row, VALUE).getNumericCellValue();
        value = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue);
        cellValue = table.getCell(row, TAX).getNumericCellValue();
        tax = (cellValue - 0.01d < 0) ? BigDecimal.ZERO : BigDecimal.valueOf(cellValue).negate();
        DividendTableRow.DividendTableRowBuilder builder = DividendTableRow.builder()
                .timestamp(convertToInstant(table.getCell(row, DATE).getStringCellValue()))
                .event(CashFlowType.DIVIDEND)
                .isin(table.getCell(row, ISIN).getStringCellValue())
                .count(Double.valueOf(table.getCell(row, COUNT).getNumericCellValue()).intValue())
                .value(value)
                .currency(table.getCell(row, VALUE_CURRENCY).getStringCellValue());
        Collection<DividendTableRow> data = new ArrayList<>();
        data.add(builder.build());
        if (!tax.equals(BigDecimal.ZERO)) {
            data.add(builder
                    .event(CashFlowType.TAX)
                    .value(tax)
                    .currency(table.getCell(row, TAX_CURRENCY).getStringCellValue())
                    .build());
        }
        return data;
    }

    enum DividendTableHeader implements TableColumnDescription {
        DATE("дата"),
        ISIN("isin"),
        COUNT("кол-во"),
        VALUE("сумма", "дивидендов"),
        VALUE_CURRENCY("валюта", "выплаты"),
        TAX("сумма", "налога"),
        TAX_CURRENCY("валюта", "налога");

        @Getter
        private final TableColumn column;
        DividendTableHeader(String... words) {
            this.column = TableColumn.of(words);
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class DividendTableRow {
        private String isin;
        private Instant timestamp;
        private CashFlowType event;
        private int count;
        private BigDecimal value; // НКД, амортизация или налог
        private String currency; // валюта
    }
}
