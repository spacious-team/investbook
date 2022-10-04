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

package ru.investbook.parser.vtb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableColumn;
import org.spacious_team.table_wrapper.api.TableColumnDescription;
import org.spacious_team.table_wrapper.api.TableColumnImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;
import static ru.investbook.parser.vtb.VtbDerivativeCashFlowTable.OpenContractsTableHeader.CONTRACT;

@Slf4j
public class VtbDerivativeCashFlowTable extends AbstractVtbCashFlowTable<SecurityEventCashFlow> {

    private static final String TABLE_NAME = "Открытые позиции по Производным финансовым инструментам";
    private final Integer contractId;

    public VtbDerivativeCashFlowTable(CashFlowEventTable cashFlowEventTable) {
        super(cashFlowEventTable);
        List<String> contracts = getReport().getReportPage()
                .create(TABLE_NAME, OpenContractsTableHeader.class)
                .getData(row -> row.getStringCellValue(CONTRACT));
        this.contractId = (contracts.size() == 1) ?
                getReport().getSecurityRegistrar().declareDerivative(contracts.get(0)) :
                null;
        if (contracts.size() > 1) {
            log.warn("""
                    Отчет {} содержит информацию о вариационной марже разных контрактов, \
                    ВТБ не указывает вариационную маржу с привязкой к контракту, \
                    вариационная маржа не может быть извлечена
                    """, getReport());
        }
    }

    @Override
    protected Collection<SecurityEventCashFlow> parseTable() {
        return contractId == null ?
                Collections.emptyList() :
                super.parseTable();
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(CashFlowEventTable.CashFlowEvent event) {
        CashFlowType eventType = event.getEventType();
        if (eventType != DERIVATIVE_PROFIT) {
            return Collections.emptyList();
        }
        return singletonList(SecurityEventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(event.getDate())
                .security(contractId)
                .eventType(eventType)
                .value(event.getValue())
                .currency(event.getCurrency())
                .build());
    }

    @Getter
    enum OpenContractsTableHeader implements TableColumnDescription {
        CONTRACT("Фьючерсный", "контракт", "опцион");

        private final TableColumn column;

        OpenContractsTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
