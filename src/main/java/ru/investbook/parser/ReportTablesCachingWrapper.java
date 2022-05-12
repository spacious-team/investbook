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

import lombok.Getter;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.ReportTables;

@Getter
public class ReportTablesCachingWrapper implements ReportTables {

    private final BrokerReport report;
    private final ReportTable<PortfolioProperty> portfolioPropertyTable;
    private final ReportTable<PortfolioCash> portfolioCashTable;
    private final ReportTable<EventCashFlow> cashFlowTable;
    private final ReportTable<Security> securitiesTable;
    private final ReportTable<AbstractTransaction> transactionTable;
    private final ReportTable<SecurityEventCashFlow> securityEventCashFlowTable;
    private final ReportTable<SecurityQuote> securityQuoteTable;
    private final ReportTable<ForeignExchangeRate> foreignExchangeRateTable;

    public ReportTablesCachingWrapper(ReportTables wrappee) {
        this.report = wrappee.getReport();
        this.portfolioPropertyTable = wrappee.getPortfolioPropertyTable();
        this.portfolioCashTable = wrappee.getPortfolioCashTable();
        this.cashFlowTable = wrappee.getCashFlowTable();
        this.securitiesTable = wrappee.getSecuritiesTable();
        this.transactionTable = wrappee.getTransactionTable();
        this.securityEventCashFlowTable = wrappee.getSecurityEventCashFlowTable();
        this.securityQuoteTable = wrappee.getSecurityQuoteTable();
        this.foreignExchangeRateTable = wrappee.getForeignExchangeRateTable();
    }
}
