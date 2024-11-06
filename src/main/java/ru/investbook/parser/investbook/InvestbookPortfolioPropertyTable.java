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

package ru.investbook.parser.investbook;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.TableRow;

import static ru.investbook.parser.investbook.AbstractInvestbookTable.InvestbookReportTableHeader.*;

public class InvestbookPortfolioPropertyTable extends AbstractInvestbookTable<PortfolioProperty> {

    protected InvestbookPortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    @Override
    protected @Nullable PortfolioProperty parseRow(TableRow row) {
        String operation = row.getStringCellValue(OPERATION).toLowerCase();
        if (!operation.contains("актив")) { // Оценка стоимости активов
            return null;
        }
        String currency = row.getStringCellValue(CURRENCY);
        PortfolioPropertyType property;
        if (currency.equalsIgnoreCase("RUB") || currency.equalsIgnoreCase("RUR")) {
            property = PortfolioPropertyType.TOTAL_ASSETS_RUB;
        } else if (currency.equalsIgnoreCase("USD")) {
            property = PortfolioPropertyType.TOTAL_ASSETS_USD;
        } else {
            throw new IllegalArgumentException(
                    "Оценка активов может быть выполнена только в RUB или USD, указана валюта: " + currency);
        }
        return PortfolioProperty.builder()
                .portfolio(row.getStringCellValue(PORTFOLIO))
                .timestamp(row.getInstantCellValue(DATE_TIME))
                .property(property)
                .value(row.getBigDecimalCellValue(PRICE).toString())
                .build();
    }
}
