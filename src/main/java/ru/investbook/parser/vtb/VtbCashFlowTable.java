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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.spacious_team.broker.pojo.CashFlowType.CASH;
import static org.spacious_team.broker.pojo.CashFlowType.TAX;

public class VtbCashFlowTable extends AbstractVtbCashFlowTable<EventCashFlow> {

    public VtbCashFlowTable(CashFlowEventTable cashFlowEventTable) {
        super(cashFlowEventTable, EventCashFlow::checkEquality, EventCashFlow::mergeDuplicates);
    }

    @Override
    protected Collection<EventCashFlow> getRow(CashFlowEventTable.CashFlowEvent event) {
        @Nullable CashFlowType type = event.getEventType();
        if (type != CASH && type != TAX) {
            return Collections.emptyList();
        }
        String description = event.getDescription();
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(event.getDate())
                .value(event.getValue())
                .currency(event.getCurrency())
                .description(StringUtils.hasLength(description) ? description : null)
                .build());
    }
}
