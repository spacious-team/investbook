/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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
import org.spacious_team.broker.pojo.*;
import org.spacious_team.broker.report_parser.api.*;

public class VtbReportTables implements ReportTables {
    @Getter
    private final BrokerReport report;
    @Getter
    private final ReportTable<Security> securitiesTable;
    @Getter
    private final VtbCouponAmortizationRedemptionTable couponAmortizationRedemptionTable;
    private final CashFlowEventTable cashFlowEventTable;
    private final VtbSecurityDepositAndWithdrawalTable vtbSecurityDepositAndWithdrawalTable;

    public VtbReportTables(BrokerReport report) {
        this.report = report;
        VtbSecuritiesTable vtbSecuritiesTable = new VtbSecuritiesTable(report);
        VtbSecurityFlowTable vtbSecurityFlowTable = new VtbSecurityFlowTable(report);
        this.securitiesTable = WrappingReportTable.of(vtbSecuritiesTable, vtbSecurityFlowTable);
        SecurityRegNumberToIsinConverter securityRegNumberToIsinConverter = new SecurityRegNumberToIsinConverterImpl(
                vtbSecuritiesTable, vtbSecurityFlowTable);
        this.cashFlowEventTable = new CashFlowEventTable(report);
        this.vtbSecurityDepositAndWithdrawalTable = new VtbSecurityDepositAndWithdrawalTable(report);
        this.couponAmortizationRedemptionTable = new VtbCouponAmortizationRedemptionTable(
                cashFlowEventTable, securityRegNumberToIsinConverter, vtbSecurityDepositAndWithdrawalTable);
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
    public ReportTable<SecurityTransaction> getSecurityTransactionTable() {
        return WrappingReportTable.of(
                new VtbSecurityTransactionTable(report),
                vtbSecurityDepositAndWithdrawalTable);
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
        return new EmptyReportTable<>(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        return new EmptyReportTable<>(report);
    }

    @Override
    public ReportTable<SecurityQuote> getSecurityQuoteTable() {
        return new VtbSecurityQuoteTable(report);
    }
}
