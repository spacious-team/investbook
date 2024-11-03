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

package ru.investbook.service.moex;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.pojo.SecurityType;

import java.util.Optional;

public interface MoexIssClient {

    Optional<String> getSecId(String isinOrContractName, SecurityType securityType);

    Optional<String> getIsin(String secId);

    Optional<MoexMarketDescription> getMarket(String moexSecId);

    Optional<SecurityQuote> getQuote(String moexSecId, MoexMarketDescription market);

    /**
     * May be false negative (return false in case of no quotes on moex iss)
     *
     * @return true if Moex hasn't quotes
     */
    boolean isDerivativeAndExpired(@Nullable String shortnameOrSecid, SecurityType securityType);

    /**
     * @param contract option's code (moex secid) in {@code Si65000BC9}, {@code Si65000BC9D}, {@code RI180000BD1} or
     *                   {@code RI180000BD1A} format
     * @return futures contract code (secid) for ex. {@code SiH9} if it can be calculated, empty optional otherwise
     */
    Optional<String> getOptionUnderlingFutures(String contract);

    /**
     * @return {@code Si-6.21M270521CA75000} for option contract in {@code Si-6.21M270521CA75000} or {@code Si75000BE1D} format;
     * {@code Si-6.21M170621PA71000} for option contract in {@code Si-6.21M170621PA71000} or {@code Si71000BR1} format
     */
    Optional<String> getOptionShortname(String contract);
}
