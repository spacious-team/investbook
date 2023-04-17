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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Slf4j
public class VtbPortfolioPropertyTable extends SingleInitializableReportTable<PortfolioProperty> {

    private static final String TOTAL_ASSETS1 = "ОЦЕНКА активов (по курсу ЦБ с учётом незавершенных сделок)";
    private static final String TOTAL_ASSETS2 = "ОЦЕНКА активов по Kурсу с учётом незавершенных сделок";

    public VtbPortfolioPropertyTable(SingleBrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        try {
            return singleton(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(getReport().getReportEndDateTime())
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                    .value(getBigDecimalValue().toString())
                    .build());
        } catch (Exception e) {
            log.debug("Не удалось распарсить свойство '{}' или '{}' из отчета {}",
                    TOTAL_ASSETS1, TOTAL_ASSETS2, getReport());
            return emptyList();
        }
    }

    private BigDecimal getBigDecimalValue() {
        Object value = getReport().getReportPage().getNextColumnValue(TOTAL_ASSETS1);
        if (value == null) {
            value = getReport().getReportPage().getNextColumnValue(TOTAL_ASSETS2);
        }
        return BigDecimal.valueOf(
                Double.parseDouble(String.valueOf(value)));
    }
}
