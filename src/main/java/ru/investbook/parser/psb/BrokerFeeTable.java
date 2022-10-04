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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.StringUtils;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

@Slf4j
public class BrokerFeeTable extends SingleAbstractReportTable<EventCashFlow> {
    private static final String LAST_ROW = "Курс валют ЦБ РФ";
    private boolean initialized = false;

    public BrokerFeeTable(SingleBrokerReport report) {
        super(report, PortfolioPropertyTable.SUMMARY_TABLE, LAST_ROW, SummaryTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> parseRowToCollection(TableRow row) {
        String description = row.getStringCellValueOrDefault(DESCRIPTION, "");
        if (!initialized) {
            if (description.toLowerCase().contains("комиссия брокера")) {
                initialized = true; // next row contains required data
            }
            return Collections.emptySet();
        }

        if (!StringUtils.hasLength(description)) {
            return Collections.emptySet();
        }

        return Stream.of(
                        getFeeCurrency(row, RUB),
                        getFeeCurrency(row, USD),
                        getFeeCurrency(row, EUR),
                        getFeeCurrency(row, GBP),
                        getFeeCurrency(row, CHF))
                .filter(e -> Math.abs(e.getValue().floatValue()) > 1e-3)
                .map(e -> toFee(e.getValue(), e.getKey(), description))
                .toList();
    }

    private Map.Entry<String, BigDecimal> getFeeCurrency(TableRow row, SummaryTableHeader currency) {
        return new AbstractMap.SimpleEntry<>(
                currency.toString(),
                row.getBigDecimalCellValueOrDefault(currency, BigDecimal.ZERO));
    }

    private EventCashFlow toFee(BigDecimal fee, String currency, String description) {
        return EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().getReportEndDateTime())
                .eventType(CashFlowType.COMMISSION)
                .value(fee)
                .currency(currency)
                .description(description)
                .build();
    }
}
