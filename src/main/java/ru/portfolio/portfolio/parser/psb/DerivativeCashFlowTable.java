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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;
import static ru.portfolio.portfolio.parser.psb.PsbBrokerReport.convertToInstant;

@Slf4j
public class DerivativeCashFlowTable {
    private static final String TABLE1_START_TEXT = "Движение стандартных контрактов";
    private static final String TABLE2_START_TEXT = "Прочие операции";
    private static final String TABLE_END_TEXT = "Итого";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<DerivativeCashFlowTableRow> data = new ArrayList<>();

    public DerivativeCashFlowTable(PsbBrokerReport report) {
        this.report = report;
        Map<String, Integer> contractCount = pasreDerivativeCountTable(report);
        if (!contractCount.isEmpty()) {
            this.data.addAll(pasreDerivativeCashFlowTable(report, contractCount));
        }
    }

    private static Map<String, Integer> pasreDerivativeCountTable(PsbBrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE1_START_TEXT, TABLE_END_TEXT, ContractCountTableHeader.class);
        List<AbstractMap.SimpleEntry<String, Integer>> data = table.getData(report.getPath(), DerivativeCashFlowTable::getCount);
        return data.stream()
                .filter(e -> e.getValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static AbstractMap.SimpleEntry<String, Integer> getCount(ExcelTable table, Row row) {
        String contract = table.getCell(row, CONTRACT).getStringCellValue();
        int incomingCount = Math.abs(table.getCellIntValue(row, INCOUMING));
        int outgoingCount = Math.abs(table.getCellIntValue(row, OUTGOING));
        int count = Math.max(incomingCount, outgoingCount);
        if (count == 0) {
            count = Math.abs(table.getCellIntValue(row, BUY)); // buyCount == cellCount
        }
        return new AbstractMap.SimpleEntry<>(contract, count);
    }

    private List<DerivativeCashFlowTableRow> pasreDerivativeCashFlowTable(PsbBrokerReport report,
                                                                          Map<String, Integer> contractCount) {
        ExcelTable table = ExcelTable.of(report.getSheet(), TABLE2_START_TEXT, TABLE_END_TEXT, DerivativeCashFlowTableHeader.class);
        return table.getDataCollection(report.getPath(), (tab, row) -> getDerivativeCashFlow(tab, row, contractCount));
    }

    private static Collection<DerivativeCashFlowTableRow> getDerivativeCashFlow(ExcelTable table, Row row, Map<String, Integer> contractCount) {
        BigDecimal value = BigDecimal.valueOf(table.getCell(row, DerivativeCashFlowTableHeader.INCOUMING).getNumericCellValue())
                .subtract(BigDecimal.valueOf(table.getCell(row, DerivativeCashFlowTableHeader.OUTGOING).getNumericCellValue()));
        DerivativeCashFlowTableRow.DerivativeCashFlowTableRowBuilder builder = DerivativeCashFlowTableRow.builder()
                .timestamp(convertToInstant(table.getCell(row, DerivativeCashFlowTableHeader.DATE).getStringCellValue()))
                .value(value)
                .currency("RUB"); // FORTS, only RUB
        String action = table.getCell(row, DerivativeCashFlowTableHeader.OPERATION).getStringCellValue().toLowerCase();
        switch (action) {
            case "вариационная маржа":
                String contract = table.getCell(row, DerivativeCashFlowTableHeader.CONTRACT)
                        .getStringCellValue()
                        .split("/")[1].trim();
                Integer count = contractCount.get(contract);
                if (count == null) {
                    throw new IllegalArgumentException("Количество открытых контрактов не найдено");
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
