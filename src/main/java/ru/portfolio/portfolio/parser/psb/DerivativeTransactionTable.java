package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeTransactionTable.FortsTableHeader.*;

@Slf4j
public class DerivativeTransactionTable extends AbstractReportTable<DerivativeTransactionTable.FortsTableRow> {
    private static final String TABLE_NAME = "Информация о заключенных сделках";
    private static final String TABLE_END_TEXT = "Итого";

    public DerivativeTransactionTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, FortsTableHeader.class);
        getData().addAll(new DerivativeExpirationTable(report).getData());
    }

    @Override
    protected Collection<FortsTableRow> getRow(ExcelTable table, Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        int count = table.getIntCellValue(row, COUNT);
        String type = table.getStringCellValue(row, TYPE).toLowerCase();
        BigDecimal value;
        switch (type) {
            case "опцион":
                value = table.getCurrencyCellValue(row, OPTION_PRICE).multiply(BigDecimal.valueOf(count));
                break;
            case "фьючерс":
                value = table.getCurrencyCellValue(row, VALUE);
                break;
            default:
                throw new IllegalArgumentException("Не известный контракт " + type);
        }
        if (isBuy) value = value.negate();
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        return singletonList(FortsTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(Long.parseLong(table.getStringCellValue(row, TRANSACTION)))
                .isin(table.getStringCellValue(row, CONTRACT))
                .count((isBuy ? 1 : -1) * count)
                .value(value)
                .commission(commission)
                .currency("RUB") // FORTS, only RUB
                .build());
    }

    enum FortsTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("№"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма срочной сделки"),
        OPTION_PRICE("цена опциона"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        FortsTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }
    }

    @Getter
    @Builder
    static class FortsTableRow {
        private long transactionId;
        private String isin;
        private Instant timestamp;
        private int count;
        private BigDecimal value; // оценочная стоиомсть в валюце цены
        private BigDecimal commission;
        private String currency; // валюта

    }
}
