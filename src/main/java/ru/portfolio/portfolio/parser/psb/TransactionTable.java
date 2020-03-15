package ru.portfolio.portfolio.parser.psb;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.ReportTable;
import ru.portfolio.portfolio.parser.TableColumn;
import ru.portfolio.portfolio.parser.TableColumnDescription;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.TransactionTable.TransactionTableHeader.*;

@Slf4j
public class TransactionTable implements ReportTable<TransactionTable.TransactionTableRow> {
    private static final String TABLE1_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами в дату заключения";
    private static final String TABLE2_START_TEXT = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, рассчитанные в отчетном периоде";
    private static final String TABLE_END_TEXT = "Итого оборот";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<TransactionTableRow> data = new ArrayList<>();

    public TransactionTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(parseTable(report, TABLE1_START_TEXT));
        this.data.addAll(parseTable(report, TABLE2_START_TEXT));
    }

    private List<TransactionTableRow> parseTable(PsbBrokerReport report, String tableName) {
        ExcelTable table = ExcelTable.of(report.getSheet(), tableName, TABLE_END_TEXT, TransactionTableHeader.class);
        return table.getDataCollection(report.getPath(), this::getTransaction);
    }

    private Collection<TransactionTableRow> getTransaction(ExcelTable table, org.apache.poi.ss.usermodel.Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .add(table.getCurrencyCellValue(row, CLEARING_COMMISSION))
                .add(table.getCurrencyCellValue(row, ITS_COMMISSION))
                .negate();
        return Collections.singletonList(TransactionTableRow.builder()
                .timestamp(report.convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(table.getLongCellValue(row, TRANSACTION))
                .isin(table.getStringCellValue(row, ISIN))
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(commission)
                .valueCurrency(table.getStringCellValue(row, VALUE_CURRENCY).replace(" ", "").split("/")[1])
                .commissionCurrency(table.getStringCellValue(row, COMMISSION_CURRENCY))
                .build());
    }

    enum TransactionTableHeader implements TableColumnDescription {
        DATE_TIME("дата и время"),
        TRANSACTION("номер сделки"),
        ISIN("isin"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта сделки"),
        ACCRUED_INTEREST("^нкд$"),
        MARKET_COMMISSION("комиссия торговой системы"),
        CLEARING_COMMISSION("клиринговая комиссия"),
        ITS_COMMISSION("комиссия за итс"),
        BROKER_COMMISSION("ком", "брокера"),
        COMMISSION_CURRENCY("валюта", "брок", "комиссии");

        @Getter
        private final TableColumn column;
        TransactionTableHeader(String ... words) {
            this.column = TableColumn.of(words);
        }
    }

    @Getter
    @Builder
    public static class TransactionTableRow {
        private long transactionId;
        private String isin;
        private Instant timestamp;
        private int count;
        private BigDecimal value; // оценочная стоиомсть в валюце цены
        private BigDecimal accruedInterest; // НКД, в валюте бумаги
        private BigDecimal commission;
        private String valueCurrency; // валюта платежа
        private String commissionCurrency; // валюта коммиссии
    }
}
