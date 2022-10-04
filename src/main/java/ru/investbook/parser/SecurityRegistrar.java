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

package ru.investbook.parser;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;

import java.util.function.Supplier;

public interface SecurityRegistrar {

    /**
     * Checks is stock with ISIN exists and creates provided if not
     *
     * @return security ID
     */
    int declareStockByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareBondByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareStockByName(String name, Supplier<SecurityBuilder> supplier);

    int declareBondByName(String name, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByName(String name, Supplier<SecurityBuilder> supplier);

    int declareStockByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareBondByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareDerivative(String code);

    /**
     * @param contract in USDRUB_TOM form
     * @return security ID
     */
    int declareCurrencyPair(String contract);

    /**
     * @return security ID
     */
    int declareAsset(String assetName, Supplier<SecurityBuilder> supplier);

    int declareSecurity(Security security);
}
