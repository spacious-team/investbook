package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.CashTable.CashTableHeader.*;

@Slf4j
public class CashTable {
    private static final String TABLE_START_TEXT = "Позиция денежных средств по биржевым площадкам";
    private static final String TABLE_END_TEXT = "ИТОГО:";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<CashTableRow> data = new ArrayList<>();

    public CashTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(pasreTable(report));
    }

    private List<CashTableRow> pasreTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE_START_TEXT, TABLE_END_TEXT, CashTableHeader.class);
        table.setDataRowOffset(3);
        return table.getDataCollection(report.getPath(), CashTable::getCash);
    }

    private static Collection<CashTableRow> getCash(ExcelTable table, Row row) {
        return singletonList(CashTableRow.builder()
                .section(table.getCell(row, SECTION).getStringCellValue())
                .value(BigDecimal.valueOf(table.getCell(row, VALUE).getNumericCellValue()))
                .currency(table.getCell(row, CURRENCY).getStringCellValue())
                .build());
    }

    enum CashTableHeader implements TableColumnDescription {
        SECTION("сектор"),
        VALUE("плановый исходящий остаток"),
        CURRENCY("валюта");

        @Getter
        private final TableColumn column;
        CashTableHeader(String ... words) {
            this.column = ru.portfolio.portfolio.parser.TableColumn.of(words);
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
