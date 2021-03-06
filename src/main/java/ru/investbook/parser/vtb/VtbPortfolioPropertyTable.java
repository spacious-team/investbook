/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;

import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Slf4j
public class VtbPortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {

    private static final String TOTAL_ASSETS = "ОЦЕНКА активов (по курсу ЦБ с учётом незавершенных сделок)";

    public VtbPortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        try {
            return singleton(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(getReport().getReportEndDateTime())
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                    .value(getReport().getReportPage().getNextColumnValue(TOTAL_ASSETS).toString())
                    .build());
        } catch (Exception e) {
            log.debug("Не удалось распарсить свойство '{}' из {}", TOTAL_ASSETS, getReport().getPath());
            return emptyList();
        }
    }
}
