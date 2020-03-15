package ru.portfolio.portfolio.parser.psb;

import lombok.AccessLevel;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends AbstractReportTable<DerivativeCashFlowTable.DerivativeCashFlowTableRow> {

    private static final String TABLE1_NAME = "Прочие операции";
    private static final String TABLE2_NAME = "Движение стандартных контрактов";
    private static final String TABLE_END_TEXT = "Итого";
    @Getter(AccessLevel.PRIVATE)
    private Map<String, Integer> contractCount = Collections.emptyMap();

    public DerivativeCashFlowTable(PsbBrokerReport report) {
        super(report, TABLE1_NAME, TABLE_END_TEXT, DerivativeCashFlowTableHeader.class);
    }

    @Override
    protected Collection<DerivativeCashFlowTableRow> pasreTable(ExcelTable table) {
        return hasOpenContract() ? super.pasreTable(table) : Collections.emptyList();
    }

    private boolean hasOpenContract() {
        ExcelTable countTable = ExcelTable.of(getReport().getSheet(), TABLE2_NAME, TABLE_END_TEXT, ContractCountTableHeader.class);
        List<AbstractMap.SimpleEntry<String, Integer>> counts = countTable.getData(getReport().getPath(), DerivativeCashFlowTable::getCount);
        this.contractCount = counts.stream()
                .filter(e -> e.getValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return !this.contractCount.isEmpty();
    }

    private static AbstractMap.SimpleEntry<String, Integer> getCount(ExcelTable table, Row row) {
        String contract = table.getStringCellValue(row, CONTRACT);
        int incomingCount = Math.abs(table.getIntCellValue(row, INCOUMING));
        int outgoingCount = Math.abs(table.getIntCellValue(row, OUTGOING));
        int count = Math.max(incomingCount, outgoingCount);
        if (count == 0) {
            count = Math.abs(table.getIntCellValue(row, BUY)); // buyCount == cellCount
        }
        return new AbstractMap.SimpleEntry<>(contract, count);
    }

    @Override
    protected Collection<DerivativeCashFlowTableRow> getRow(ExcelTable table, Row row) {
        BigDecimal value = table.getCurrencyCellValue(row, DerivativeCashFlowTableHeader.INCOUMING)
                .subtract(table.getCurrencyCellValue(row, DerivativeCashFlowTableHeader.OUTGOING));
        DerivativeCashFlowTableRow.DerivativeCashFlowTableRowBuilder builder = DerivativeCashFlowTableRow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DerivativeCashFlowTableHeader.DATE)))
                .value(value)
                .currency("RUB"); // FORTS, only RUB
        String action = table.getCell(row, DerivativeCashFlowTableHeader.OPERATION).getStringCellValue().toLowerCase();
        switch (action) {
            case "вариационная маржа":
                String contract = table.getStringCellValue(row, DerivativeCashFlowTableHeader.CONTRACT)
                        .split("/")[1].trim();
                Integer count = getContractCount().get(contract);
                if (count == null) {
                    throw new IllegalArgumentException("Открытых контрактов не найдено");
                }
                return singletonList(builder.event(CashFlowType.DERIVATIVE_PROFIT)
                        .contract(contract)
                        .count(count)
                        .build());
            case "биржевой сбор":
                return singletonList(builder.event(CashFlowType.COMMISSION).build());
            case "заблокированo / разблокировано средств под го":
                return emptyList(); // не влияет на размер собственных денежных средств
            default:
                throw new IllegalArgumentException("Неизвестный вид операции " + action);
        }
    }

    enum ContractCountTableHeader implements TableColumnDescription {
        CONTRACT("контракт"),
        INCOUMING("входящий остаток"),
        OUTGOING("исходящий остаток"),
        BUY("зачислено"),
        CELL("списано");

        @Getter
        private final TableColumn column;
        ContractCountTableHeader(String... words) {
            this.column = TableColumn.of(words);
        }
    }

    enum DerivativeCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        CONTRACT("№", "контракт"),
        OPERATION("вид операции"),
        INCOUMING("зачислено"),
        OUTGOING("списано");

        @Getter
        private final TableColumn column;
        DerivativeCashFlowTableHeader(String... words) {
            this.column = TableColumn.of(words);
        }
    }

    @Getter
    @Builder
    public static class DerivativeCashFlowTableRow {
        private String contract;
        private Instant timestamp;
        private CashFlowType event;
        private Integer count;
        private BigDecimal value;
        private String currency; // валюта
    }
}
