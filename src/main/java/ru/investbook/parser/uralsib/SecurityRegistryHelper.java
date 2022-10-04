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

package ru.investbook.parser.uralsib;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityType;
import ru.investbook.parser.SecurityRegistrar;

import static ru.investbook.entity.SecurityEntity.isinPattern;

class SecurityRegistryHelper {

    static int declareStockOrBond(String isin, String name, SecurityRegistrar registrar) {
        Security security = getStockOrBond(isin, name);
        return declareStockOrBond(security, registrar);
    }

    static int declareStockOrBond(Security security, SecurityRegistrar registrar) {
        return (security.getTicker() != null) ?
                registrar.declareStockOrBondByTicker(security.getTicker(), security::toBuilder) :
                registrar.declareStockOrBondByIsin(security.getIsin(), security::toBuilder);
    }

    static Security getStockOrBond(String isin, String name) {
        String ticker = null;
        if (!isinPattern.matcher(isin).matches()) {
            ticker = isin;
            isin = null; // отчет иногда вместо ISIN содержит непонятный идентификаторБ например TRAK
        }
        return Security.builder()
                .isin(isin)
                .ticker(ticker)
                .name(name)
                .type(SecurityType.STOCK_OR_BOND)
                .build();
    }
}
