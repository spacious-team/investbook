package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.AbstractReportTable;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeExpirationTable.ExpirationTableHeader.*;

@Slf4j
class DerivativeExpirationTable extends AbstractReportTable<DerivativeTransactionTable.FortsTableRow> {
    private static final String TABLE_NAME = "Исполнение контрактов";
    private static final String TABLE_END_TEXT = "Итого";

    DerivativeExpirationTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, ExpirationTableHeader.class);
    }

    @Override
    protected Collection<DerivativeTransactionTable.FortsTableRow> getRow(ExcelTable table, Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        int count = table.getIntCellValue(row, COUNT);
        String type = table.getStringCellValue(row, TYPE).toLowerCase();
        BigDecimal value;
        if ("фьючерс".equals(type)) {
            value = table.getCurrencyCellValue(row, VALUE);
        } else {
            throw new IllegalArgumentException("Не известный контракт '" + type + "'"); // unexpected contract
        }
        if (isBuy) value = value.negate();
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .negate();
        return singletonList(DerivativeTransactionTable.FortsTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(table.getLongCellValue(row, TRANSACTION))
                .isin(table.getStringCellValue(row, CONTRACT))
                .count(count)
                .value(value)
                .commission(commission)
                .currency("RUB") // FORTS, only RUB
                .build());
    }

    enum ExpirationTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("номер сделки"),
        TYPE("вид контракта"),
        CONTRACT("контракт"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма"),
        MARKET_COMMISSION("комиссия торговой системы"),
        BROKER_COMMISSION("комиссия брокера");

        @Getter
        private final TableColumn column;
        ExpirationTableHeader(String ... words) {
            this.column = TableColumn.of(words);
        }
    }
}
