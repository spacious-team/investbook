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

import lombok.Getter;
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
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.service.moex.MoexDerivativeCodeService;

public class PsbReportTables extends AbstractReportTables<PsbBrokerReport> {

    private final SecurityTransactionTable securityTransactionTable;

    @Getter
    private final ReportTable<Security> securitiesTable;

    protected PsbReportTables(PsbBrokerReport report, MoexDerivativeCodeService moexDerivativeCodeService) {
        super(report);
        this.securityTransactionTable = new SecurityTransactionTable(report);
        this.securitiesTable = WrappingReportTable.of(
                new SecuritiesTable(report),
                new DerivativesTable(report, moexDerivativeCodeService),
                WrappingReportTable.of(report, securityTransactionTable.getSecurities()));
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new PortfolioPropertyTable(report);
    }

    @Override
    public ReportTable<PortfolioCash> getPortfolioCashTable() {
        return new CashTable(report);
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new CashFlowTable(report);
    }

    @Override
    public ReportTable<AbstractTransaction> getTransactionTable() {
        return WrappingReportTable.of(
                securityTransactionTable,
                new DerivativeTransactionTable(report));
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getSecurityEventCashFlowTable() {
        return WrappingReportTable.of(
                new CouponAmortizationRedemptionTable(report),
                new DividendTable(report),
                new DerivativeCashFlowTable(report));
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
