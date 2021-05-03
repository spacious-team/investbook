/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.psb;

import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;

public class PsbReportTables extends AbstractReportTables<PsbBrokerReport> {

    protected PsbReportTables(PsbBrokerReport report) {
        super(report);
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new PortfolioPropertyTable(report);
    }

    @Override
    public ReportTable<PortfolioCash> getCashTable() {
        return new CashTable(report);
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new CashFlowTable(report);
    }
    
    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return WrappingReportTable.of(
                new SecuritiesTable(report),
                new DerivativesTable(report));
    }
    
    @Override
    public ReportTable<SecurityTransaction> getSecurityTransactionTable() {
        return new SecurityTransactionTable(report);
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return new DerivativeTransactionTable(report);
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getCouponAmortizationRedemptionTable() {
        return new CouponAmortizationRedemptionTable(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDividendTable() {
        return new DividendTable(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        return new DerivativeCashFlowTable(report);
    }

    @Override
    public ReportTable<SecurityQuote> getSecurityQuoteTable() {
        return WrappingReportTable.of(
                new SecurityQuoteTable(report),
                new DerivativeQuoteTable(report));
    }

    @Override
    public ReportTable<ForeignExchangeRate> getForeignExchangeRateTable() {
        return new ForeignExchangeRateTable(report);
    }
}
