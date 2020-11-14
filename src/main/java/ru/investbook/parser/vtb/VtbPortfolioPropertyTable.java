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

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;

import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Slf4j
public class VtbPortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {

    private static final String TOTAL_ASSETS = "ОЦЕНКА активов (по курсу ЦБ с учётом незавершенных сделок)";
    private static final String USD_EXCHANGE_RATE = "Курс USD (по курсу ЦБ на конечную дату отчета)";
    private static final String EUR_EXCHANGE_RATE = "Курс EUR (по курсу ЦБ на конечную дату отчета)";
    private static final String CHF_EXCHANGE_RATE = "Курс CHF (по курсу ЦБ на конечную дату отчета)";
    private static final String GBP_EXCHANGE_RATE = "Курс GBP (по курсу ЦБ на конечную дату отчета)";

    public VtbPortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        Collection<PortfolioProperty> data = new ArrayList<>();
        data.addAll(buildPortfolioProperty(
                PortfolioPropertyType.TOTAL_ASSETS,
                TOTAL_ASSETS));
        data.addAll(buildPortfolioProperty(
                PortfolioPropertyType.USDRUB_EXCHANGE_RATE,
                USD_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                PortfolioPropertyType.EURRUB_EXCHANGE_RATE,
                EUR_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                PortfolioPropertyType.CHFRUB_EXCHANGE_RATE,
                CHF_EXCHANGE_RATE));
        data.addAll(buildPortfolioProperty(
                PortfolioPropertyType.GBPRUB_EXCHANGE_RATE,
                GBP_EXCHANGE_RATE));
        return data;
    }

    private Collection<PortfolioProperty> buildPortfolioProperty(PortfolioPropertyType property, String rowHeader) {
        try {
            return singleton(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(getReport().getReportEndDateTime())
                    .property(property)
                    .value(getReport().getReportPage().getNextColumnValue(rowHeader).toString())
                    .build());
        } catch (Exception e) {
            log.info("Не удалось распарсить свойство '{}' из {}", rowHeader, getReport().getPath());
            return emptyList();
        }
    }
}
