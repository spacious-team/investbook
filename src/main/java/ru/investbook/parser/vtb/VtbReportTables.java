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

package ru.investbook.parser.vtb;

import lombok.Getter;
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
import ru.investbook.parser.SingleBrokerReport;

public class VtbReportTables extends AbstractReportTables<SingleBrokerReport> {

    @Getter
    private final ReportTable<Security> securitiesTable;
    @Getter
    private final ReportTable<SecurityTransaction> securityTransactionTable;
    @Getter
    private final VtbCouponAmortizationRedemptionTable couponAmortizationRedemptionTable;
    private final CashFlowEventTable cashFlowEventTable;

    public VtbReportTables(SingleBrokerReport report) {
        super(report);
        VtbSecuritiesTable securitiesTable = new VtbSecuritiesTable(report);
        VtbSecurityFlowTable securityFlowTable = new VtbSecurityFlowTable(report);
        VtbSecurityTransactionTable securityTransactionTable = new VtbSecurityTransactionTable(report);
        this.securitiesTable = WrappingReportTable.of(
                securitiesTable,
                securityFlowTable,
                WrappingReportTable.of(report, securityTransactionTable.getSecurities()));
        SecurityRegNumberRegistrar securityRegNumberRegistrar = new SecurityRegNumberRegistrarImpl(
                securitiesTable, securityFlowTable);
        this.cashFlowEventTable = new CashFlowEventTable(report);
        VtbSecurityDepositAndWithdrawalTable securityDepositAndWithdrawalTable =
                new VtbSecurityDepositAndWithdrawalTable(report);
        this.securityTransactionTable = WrappingReportTable.of(
                securityTransactionTable,
                securityDepositAndWithdrawalTable);
        this.couponAmortizationRedemptionTable = new VtbCouponAmortizationRedemptionTable(
                cashFlowEventTable, securityRegNumberRegistrar, securityDepositAndWithdrawalTable);
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new VtbPortfolioPropertyTable(report);
    }

    @Override
    public ReportTable<PortfolioCash> getCashTable() {
        return new VtbCashTable(report);
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return WrappingReportTable.of(
                new VtbCashFlowTable(cashFlowEventTable),
                new VtbDividendTable(cashFlowEventTable),
                WrappingReportTable.of(report, couponAmortizationRedemptionTable.getExternalBondPayments()));
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return new VtbDerivativeTransactionTable(report);
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return new VtbForeignExchangeTransactionTable(report);
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
        return new VtbSecurityQuoteTable(report);
    }

    @Override
    public ReportTable<ForeignExchangeRate> getForeignExchangeRateTable() {
        return new VtbForeignExchangeRateTable(report);
    }
}
