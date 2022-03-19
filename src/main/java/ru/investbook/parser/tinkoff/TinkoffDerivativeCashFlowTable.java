/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.TinkoffDerivativeCashFlowTable.DerivativeCashFlowTableHeader.*;

public class TinkoffDerivativeCashFlowTable extends SingleAbstractReportTable<SecurityEventCashFlow>  {

    protected TinkoffDerivativeCashFlowTable(SingleBrokerReport report) {
        super(report,
                (cell) -> cell.startsWith("3.3 Информация о позиционном состоянии по производным финансовым инструментам"),
                (cell) -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                DerivativeCashFlowTableHeader.class);
    }

    @Override
    protected SecurityEventCashFlow parseRow(TableRow row) {
        String contract = row.getStringCellValueOrDefault(CONTRACT, null);
        if (!hasLength(contract) || contract.contains("Наименование")) {
            return null;
        }
        int count = Math.max(
            Math.abs(row.getIntCellValueOrDefault(INCOMING_COUNT, 0)),
            Math.abs(row.getIntCellValueOrDefault(OUTGOING_COUNT, 0)));
        return SecurityEventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().convertToInstant(row.getStringCellValue(DATE)))
                .security(getReport().getSecurityRegistrar().declareDerivative(contract))
                .count(count)
                .eventType(CashFlowType.DERIVATIVE_PROFIT)
                .value(row.getBigDecimalCellValue(VARIATION_MARGIN))
                .currency("RUB")
                .build();
    }

    @RequiredArgsConstructor
    protected enum DerivativeCashFlowTableHeader implements TableColumnDescription {
        DATE("Дата"),
        CONTRACT("Наименование", "контракта"),
        INCOMING_COUNT("Позиция", "на", "начало", "дня"),
        OUTGOING_COUNT("Позиция", "на", "конец", "дня"),
        VARIATION_MARGIN("Вар", "маржа", "на", "конец", "дня");

        @Getter
        private final TableColumn column;

        DerivativeCashFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
