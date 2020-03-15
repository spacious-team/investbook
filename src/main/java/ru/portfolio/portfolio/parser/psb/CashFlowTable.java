package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.CashFlowTable.CashFlowTableHeader.*;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class CashFlowTable {
    private static final String TABLE_START_TEXT = "Внешнее движение денежных средств в валюте счета";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<CashFlowTableRow> data = new ArrayList<>();

    public CashFlowTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report));
    }

    private List<CashFlowTableRow> pasreTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE_START_TEXT, CashFlowTableHeader.class);
        return table.getDataCollection(report.getPath(), CashFlowTable::getCash);
    }

    private static Collection<CashFlowTableRow> getCash(ExcelTable table, Row row) {
        String action = table.getCell(row, OPERATION).getStringCellValue();
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
                .timestamp(convertToInstant(table.getCell(row, DATE).getStringCellValue()))
                .type(type)
                .value(BigDecimal.valueOf((isPositive ? 1 : -1) * table.getCell(row, VALUE).getNumericCellValue()))
                .currency(table.getCell(row, CURRENCY).getStringCellValue())
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
            this.column = ru.portfolio.portfolio.parser.TableColumn.of(words);
        }
    }

    @Getter
    @Builder
    public static class CashFlowTableRow {
        private Instant timestamp;
        private CashFlowType type;
        private BigDecimal value;
        private String currency; // валюта
    }
}
