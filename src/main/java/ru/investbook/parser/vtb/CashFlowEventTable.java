/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.vtb;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.*;
import org.spacious_team.table_wrapper.excel.ExcelTable;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.investbook.parser.vtb.CashFlowEventTable.VtbCashFlowTableHeader.*;

public class CashFlowEventTable extends AbstractReportTable<CashFlowEventTable.CashFlowEvent> {
    private static final String TABLE_NAME = "Движение денежных средств";
    private boolean isSubaccountPaymentsRemoved = false;

    public CashFlowEventTable(BrokerReport report) {
        super(report, TABLE_NAME, null, VtbCashFlowTableHeader.class);
    }

    @Override
    protected Collection<CashFlowEvent> getRow(Table table, TableRow row) {
        String operation = table.getStringCellValueOrDefault(row, OPERATION, null);
        if (operation == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(CashFlowEvent.builder()
                .date(((ExcelTable) table).getDateCellValue(row, DATE).toInstant())
                .operation(operation.toLowerCase().trim())
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .description(table.getStringCellValueOrDefault(row, DESCRIPTION, ""))
                .build());
    }

    @Override
    public List<CashFlowEvent> getData() {
        List<CashFlowEvent> data = super.getData();
        if (!isSubaccountPaymentsRemoved) {
            // gh-170: дивиденды и купоны субсчета, проходят через основный счет.
            // Для основного счета удаляем события выплаты + перечисляения на субсчет
            Collection<CashFlowEvent> filteredData = new ArrayList<>(data.size());
            for (CashFlowEvent event : data) {
                if (data.stream().noneMatch(e -> e.isSubaccountPaymentEvent(event))) {
                    filteredData.add(event);
                }
            }
            data.clear();
            data.addAll(filteredData);
            isSubaccountPaymentsRemoved = true;
        }
        return data;
    }

    @Builder
    @Getter
    @EqualsAndHashCode
    static class CashFlowEvent {
        private static final String duplicateOperation = "Перераспределение дохода между субсчетами / торговыми площадками";
        private final Instant date;
        private final BigDecimal value;
        private final String currency;
        private final String operation;
        private final String description;

        boolean isSubaccountPaymentEvent(CashFlowEvent pairedEvent) {
            return date.equals(pairedEvent.date) &&
                    (duplicateOperation.equals(operation) || duplicateOperation.equals(pairedEvent.operation)) &&
                    value.equals(pairedEvent.value.negate()) &&
                    currency.equals(pairedEvent.currency) &&
                    ((StringUtils.isEmpty(description)) ?
                            StringUtils.isEmpty(pairedEvent.description) :
                            description.equals(pairedEvent.description));
        }
    }

    @Getter
    enum VtbCashFlowTableHeader implements TableColumnDescription {
        DATE("дата"),
        VALUE("сумма"),
        CURRENCY("валюта"),
        OPERATION("тип операции"),
        DESCRIPTION("комментарий");

        private final TableColumn column;

        VtbCashFlowTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
