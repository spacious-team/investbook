/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.tinkoff;

import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;

public class TinkoffReportTables extends AbstractReportTables<TinkoffBrokerReport> {
    
    protected TinkoffReportTables(TinkoffBrokerReport report) {
        super(report);
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<PortfolioCash> getPortfolioCashTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<AbstractTransaction> getTransactionTable() {
        SecurityCodeAndIsinTable securityCodeAndIsinTable = new SecurityCodeAndIsinTable(report);
        return new TinkoffSecurityTransactionTable(report, securityCodeAndIsinTable);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getSecurityEventCashFlowTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityQuote> getSecurityQuoteTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<ForeignExchangeRate> getForeignExchangeRateTable() {
        return emptyTable();
    }
}
