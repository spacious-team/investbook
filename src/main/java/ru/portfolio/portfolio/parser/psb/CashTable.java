package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.CashTable.CashTableHeader.*;

@Slf4j
public class CashTable extends AbstractReportTable<CashTable.CashTableRow> {

    private static final String TABLE_NAME = "Позиция денежных средств по биржевым площадкам";
    private static final String TABLE_END_TEXT = "ИТОГО:";

    public CashTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, CashTableHeader.class);
    }

    @Override
    protected void exelTableConfiguration(ExcelTable table) {
        table.setDataRowOffset(3);
    }

    @Override
    protected Collection<CashTableRow> getRow(ExcelTable table, Row row) {
        return singletonList(CashTableRow.builder()
                .section(table.getStringCellValue(row, SECTION))
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(table.getStringCellValue(row, CURRENCY))
                .build());
    }

    enum CashTableHeader implements TableColumnDescription {
        SECTION("сектор"),
        VALUE("плановый исходящий остаток"),
        CURRENCY("валюта");

        @Getter
        private final TableColumn column;
        CashTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @Builder
    static class CashTableRow {
        private String section;
        private BigDecimal value;
        private String currency;
    }
}
