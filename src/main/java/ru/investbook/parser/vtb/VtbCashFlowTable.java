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

import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;

public class VtbCashFlowTable extends AbstractVtbCashFlowTable<EventCashFlow> {

    public VtbCashFlowTable(CashFlowEventTable cashFlowEventTable) {
        super(cashFlowEventTable);
    }

    @Override
    protected Collection<EventCashFlow> getRow(CashFlowEventTable.CashFlowEvent event) {
        CashFlowType type = getType(event);
        if (type == null) {
            return Collections.emptyList();
        }
        String description = event.getDescription();
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(event.getDate())
                .value(event.getValue())
                .currency(event.getCurrency())
                .description(StringUtils.isEmpty(description) ? null : description)
                .build());
    }

    private CashFlowType getType(CashFlowEventTable.CashFlowEvent event) {
        return switch (event.getOperation()) {
            // gh-170
            case "зачисление денежных средств" -> isCash(event.getDescription()) ? CashFlowType.CASH : null;
            case "списание денежных средств" -> CashFlowType.CASH;
            case "перевод денежных средств" -> CashFlowType.CASH; // перевод ДС на другой субсчет
            case "перераспределение дохода между субсчетами / торговыми площадками" -> CashFlowType.CASH; // выплаты субсчета проходят через основной счет
            case "ндфл" -> CashFlowType.TAX;
            default -> null;
        };
    }

    private boolean isCash(String description) {
        String lowercaseDescription = description.toLowerCase();
        return !(lowercaseDescription.contains("погаш. номин.ст-ти обл") ||
                lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч") ||
                lowercaseDescription.contains("куп. дох. по обл") ||
                lowercaseDescription.contains("дивиденды"));
    }

    @Override
    protected boolean checkEquality(EventCashFlow flow1, EventCashFlow flow2) {
        return EventCashFlow.checkEquality(flow1, flow2);
    }

    @Override
    protected Collection<EventCashFlow> mergeDuplicates(EventCashFlow old, EventCashFlow nw) {
        return EventCashFlow.mergeDuplicates(old, nw);
    }
}
