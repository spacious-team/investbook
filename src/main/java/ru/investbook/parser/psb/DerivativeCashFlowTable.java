/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.psb;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.PatternTableColumn;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableHeaderColumn;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.investbook.parser.psb.DerivativeCashFlowTable.ContractCountTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends SingleAbstractReportTable<SecurityEventCashFlow> {

    private static final String TABLE1_NAME = "Прочие операции";
    static final String TABLE2_NAME = "Движение стандартных контрактов";
    static final String TABLE_END_TEXT = "Итого";
    @Getter(AccessLevel.PRIVATE)
    private Map<String, Integer> contractCount = Collections.emptyMap();

    public DerivativeCashFlowTable(PsbBrokerReport report) {
        super(report, TABLE1_NAME, TABLE_END_TEXT, DerivativeCashFlowTableHeader.class);
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseTable(Table table) {
        return hasOpenContract() ? super.parseTable(table) : Collections.emptyList();
    }

    private boolean hasOpenContract() {
        contractCount = getReport().getReportPage()
                .create(TABLE2_NAME, TABLE_END_TEXT, ContractCountTableHeader.class)
                .excludeTotalRow()
                .getData(getReport(), DerivativeCashFlowTable::getCount)
                .stream()
                .filter(e -> e.getValue() != 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return !contractCount.isEmpty();
    }

    private static AbstractMap.SimpleEntry<String, Integer> getCount(TableRow row) {
        String contract = row.getStringCellValue(CONTRACT);
        int incomingCount = Math.abs(row.getIntCellValue(INCOMING));
        int outgoingCount = Math.abs(row.getIntCellValue(OUTGOING));
        int count = Math.max(incomingCount, outgoingCount);
        if (count == 0) {
            count = Math.abs(row.getIntCellValue(BUY)); // buyCount == cellCount
        }
        return new AbstractMap.SimpleEntry<>(contract, count);
    }

    @Override
    protected @Nullable SecurityEventCashFlow parseRow(TableRow row) {
        BigDecimal value = row.getBigDecimalCellValue(DerivativeCashFlowTableHeader.INCOMING)
                .subtract(row.getBigDecimalCellValue(DerivativeCashFlowTableHeader.OUTGOING));
        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DerivativeCashFlowTableHeader.DATE)))
                .portfolio(getReport().getPortfolio())
                .value(value)
                .currency("RUB"); // FORTS, only RUB
        String action = row.getStringCellValue(DerivativeCashFlowTableHeader.OPERATION).toLowerCase();
        switch (action) {
            case "вариационная маржа":
                String contract = row.getStringCellValue(DerivativeCashFlowTableHeader.CONTRACT)
                        .split("/")[1].trim();
                Integer count = getContractCount().get(contract);
                if (count == null) {
                    throw new IllegalArgumentException("Открытых контрактов не найдено");
                }
                int securityId = getReport().getSecurityRegistrar().declareDerivative(contract);
                return builder.eventType(CashFlowType.DERIVATIVE_PROFIT)
                        .security(securityId)
                        .count(count)
                        .build();
            case "биржевой сбор":
                return null; // изменения отображаются в ликвидной стоимости портфеля
            // латиница в слове "заблокированo" - это опечатка в брокерском отчёте
            case "заблокированo / разблокировано средств под го":
                return null; // не влияет на размер собственных денежных средств
            default:
                throw new IllegalArgumentException("Неизвестный вид операции " + action);
        }
    }

    @Getter
    enum ContractCountTableHeader implements TableHeaderColumn {
        CONTRACT("^контракт$"),
        INCOMING("входящий остаток"),
        OUTGOING("исходящий остаток"),
        BUY("зачислено"),
        CELL("списано"),
        PRICE("цена закрытия", "руб"),
        PRICE_TICK("шаг цены"),
        PRICE_TICK_VALUE("стоимость шага цены");

        private final TableColumn column;

        ContractCountTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }

    @Getter
    enum DerivativeCashFlowTableHeader implements TableHeaderColumn {
        DATE("дата"),
        CONTRACT("№", "контракт"),
        OPERATION("вид операции"),
        INCOMING("зачислено"),
        OUTGOING("списано");

        private final TableColumn column;

        DerivativeCashFlowTableHeader(String... words) {
            this.column = PatternTableColumn.of(words);
        }
    }
}
