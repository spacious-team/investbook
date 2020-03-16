package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.portfolio.portfolio.parser.ExcelTableHelper.rowContains;
import static ru.portfolio.portfolio.parser.psb.PortfolioSecuritiesTable.PortfolioSecuritiesTableHeader.*;

@Slf4j
public class PortfolioSecuritiesTable extends AbstractReportTable<PortfolioSecuritiesTable.PortfolioSecuritiesTableRow> {
    private static final String TABLE_NAME = "Портфель на конец дня на биржевом рынке";
    private static final String TABLE_END_TEXT = "* цена последней сделки (на организованных торгах)";
    private static final String INVALID_TEXT = "Итого в валюте цены";

    public PortfolioSecuritiesTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, PortfolioSecuritiesTableHeader.class);
    }

    @Override
    protected Collection<PortfolioSecuritiesTableRow> getRow(ExcelTable table, Row row) {
        return rowContains(table.getSheet(), row.getRowNum(), INVALID_TEXT) ?
                emptyList() :
                getPosition(table, row);
    }

    private static Collection<PortfolioSecuritiesTableRow> getPosition(ExcelTable table, Row row) {
        String currency = table.getStringCellValue(row, CURRENCY);
        return Collections.singletonList(PortfolioSecuritiesTableRow.builder()
                .name(table.getStringCellValue(row, NAME))
                .isin(table.getStringCellValue(row, ISIN))
                .buyCount(table.getIntCellValue(row, BUY))
                .cellCount(table.getIntCellValue(row, CELL))
                .outgoingCount(table.getIntCellValue(row, OUTGOING))
                .currency((currency != null && !currency.isEmpty()) ? currency : "RUB")
                .amount(table.getCurrencyCellValue(row, AMOUNT))
                .accruedInterest(table.getCurrencyCellValue(row, ACCRUED_INTEREST))
                .build());
    }

    enum PortfolioSecuritiesTableHeader implements TableColumnDescription {
        NAME("наименование"),
        ISIN("isin"),
        OUTGOING("исходящий", "остаток"),
        BUY("зачислено"),
        CELL("списано"),
        AMOUNT("оценочная стоимость в валюте цены"),
        ACCRUED_INTEREST("нкд"),
        CURRENCY("валюта цены");

        @Getter
        private final TableColumn column;
        PortfolioSecuritiesTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @Builder
    public static class PortfolioSecuritiesTableRow {
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
