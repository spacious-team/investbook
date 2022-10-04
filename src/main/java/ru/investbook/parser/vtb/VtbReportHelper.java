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

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;

class VtbReportHelper {

    static Security getSecurity(String description) {
        String[] parts = description.split(",");
        String name = parts[0].trim();
        SecurityType type = SecurityType.STOCK_OR_BOND;
        String ticker = null;
        if (name.endsWith(" US Equity") || name.endsWith(" US")) {
            ticker = name.substring(0, name.lastIndexOf(" US"));
            name = null;
            type = SecurityType.STOCK;
        }
        String isin = parts[2].toUpperCase().trim();
        return Security.builder()
                .isin(isin)
                .ticker(ticker)
                .name(name)
                .type(type)
                .build();
    }
}