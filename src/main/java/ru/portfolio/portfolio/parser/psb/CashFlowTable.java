package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.CashFlowType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.CashFlowTable.CashFlowTableHeader.*;

@Slf4j
public class CashFlowTable extends AbstractReportTable<CashFlowTable.CashFlowTableRow> {

    private static final String TABLE_NAME = "Внешнее движение денежных средств в валюте счета";

    public CashFlowTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, "", CashFlowTableHeader.class);
    }

    @Override
    protected Collection<CashFlowTableRow> pasreTable(ExcelTable table) {
        return table.getDataCollection(getReport().getPath(), this::getRow, e ->
                // SQL db restricts storing duplicate rows. Join rows by summing they values.
                Collections.singletonList(e.toBuilder()
                        .value(e.getValue()
                                .multiply(BigDecimal.valueOf(2)))
                        .build()));
    }

    @Override
    protected Collection<CashFlowTableRow> getRow(ExcelTable table, Row row) {
        String action = table.getStringCellValue(row, OPERATION);
        CashFlowType type = CashFlowType.CASH;
        boolean isPositive;
        if (action.equalsIgnoreCase("Зачислено на счет")) {
            isPositive = true;
        } else if (action.equalsIgnoreCase("Списано со счета")) {
            isPositive = false;
        } else if (action.equalsIgnoreCase("Налог удержанный")) {
            isPositive = false;
            type = CashFlowType.TAX;
        } else {
            return emptyList();
        }
        if (type == CashFlowType.CASH && !isDescriptionEmpty(table, row)) {
            return emptyList(); // cash in/out records has no description
        }
        return singletonList(CashFlowTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .type(type)
                .value(table.getCurrencyCellValue(row, VALUE)
                        .multiply(BigDecimal.valueOf(isPositive ? 1 : -1)))
                .currency(table.getStringCellValue(row, CURRENCY))
                .build());
    }

    private static boolean isDescriptionEmpty(ExcelTable table, Row row) {
        Cell cell = table.getCell(row, DESCRIPTION);
        return cell == null ||
                cell.getCellType() == CellType.BLANK ||
                (cell.getCellType() == CellType.STRING && cell.getStringCellValue().isEmpty());
    }

    enum CashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        OPERATION("операция"),
        VALUE("сумма"),
        CURRENCY("валюта счета"),
        DESCRIPTION("комментарий");

        @Getter
        private final TableColumn column;
        CashFlowTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    public static class CashFlowTableRow {
        private Instant timestamp;
        private CashFlowType type;
        private BigDecimal value;
        private String currency; // валюта
    }
}
