/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.vtb;

import ru.investbook.parser.BrokerReport;
import ru.investbook.parser.InitializableReportTable;
import ru.investbook.parser.table.ReportPage;
import ru.investbook.pojo.PortfolioProperty;
import ru.investbook.pojo.PortfolioPropertyType;

import java.util.ArrayList;
import java.util.Collection;

public class VtbPortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {

    private static final String TOTAL_ASSETS = "ОЦЕНКА активов (по курсу ЦБ с учётом незавершенных сделок)";
    private static final String USD_EXCHANGE_RATE = "Курс USD (по курсу ЦБ на конечную дату отчета)";

    public VtbPortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        Collection<PortfolioProperty> data = new ArrayList<>();
        ReportPage reportPage = getReport().getReportPage();
        data.add(buildPortfolioProperty(
                        PortfolioPropertyType.TOTAL_ASSETS,
                        reportPage.getNextColumnValue(TOTAL_ASSETS).toString()));
        data.add(buildPortfolioProperty(
                        PortfolioPropertyType.USDRUB_EXCHANGE_RATE,
                        reportPage.getNextColumnValue(USD_EXCHANGE_RATE).toString()));
        return data;
    }

    private PortfolioProperty buildPortfolioProperty(PortfolioPropertyType property, String value) {
        return PortfolioProperty.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().getReportEndDateTime())
                .property(property)
                .value(value)
                .build();
    }
}
