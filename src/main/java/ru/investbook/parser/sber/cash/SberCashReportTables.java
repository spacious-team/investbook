/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.sber.cash;

import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;

public class SberCashReportTables extends AbstractReportTables<BrokerReport> {

    protected SberCashReportTables(BrokerReport report) {
        super(report);
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<PortfolioCash> getCashTable() {
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
    public ReportTable<SecurityTransaction> getSecurityTransactionTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getCouponAmortizationRedemptionTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getDividendTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
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
