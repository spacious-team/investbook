/*
 * InvestBook
 * Copyright (C) 2024  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.api;

public class ApiUtil {
    private ApiUtil(){}
    public static final String DEFAULT_EVENT_CASH_FLOW_SORT_BY = "portfolio";
    public static final String DEFAULT_FOREIGN_EXCHANGE_RATE_SORT_BY = "rate";
    public static final String DEFAULT_ISSUER_SORT_BY = "taxpayerId";
    public static final String DEFAULT_PORTFOLIO_PROPERTY_SORT_BY = "portfolio";
    public static final String DEFAULT_PORTFOLIO_CASH_SORT_BY = "portfolio";
    public static final String DEFAULT_SECURITY_DESCRIPTION_SORT_BY= "sector";
    public static final String DEFAULT_SECURITY_EVENT_CASH_FLOW_SORT_BY = "value";
    public static final String DEFAULT_SECURITY_QUOTE_SORT_BY = "quote";
    public static final String DEFAULT_SECURITY_SORT_BY = "name";
    public static final String DEFAULT_TRANSACTION_CASH_FLOW_SORT_BY = "transactionId";
    public static final String DEFAULT_TRANSACTION_SORT_BY = "tradeId";
    public static final String DEFAULT_PORTFOLIO_SORT_BY = "id";
}
