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

package ru.investbook.parser.investbook;

import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.spacious_team.broker.pojo.CashFlowType.*;

public class InvestbookReportTables extends AbstractReportTables<BrokerReport> {

    private final ReportTable<AbstractTransaction> transactionTable;
    private final ReportTable<SecurityTransaction> depositAndWithdrawalTable;
    private final ReportTable<SecurityEventCashFlow> securityEventCashFlowTable;

    protected InvestbookReportTables(BrokerReport report,
                                     SecurityRegistrar securityRegistrar,
                                     SecurityRepository securityRepository,
                                     SecurityConverter securityConverter) {
        super(report);
        this.transactionTable = new InvestbookTransactionTable(
                getReport(), securityRegistrar, securityRepository, securityConverter);
        this.depositAndWithdrawalTable = new InvestbookSecurityDepositAndWithdrawalTable(
                getReport(), securityRegistrar, securityRepository, securityConverter);
        this.securityEventCashFlowTable = new InvestbookSecurityEventCashFowTable(
                getReport(), securityRegistrar, securityRepository, securityConverter);
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new InvestbookPortfolioPropertyTable(getReport());
    }

    @Override
    public ReportTable<PortfolioCash> getCashTable() {
        return new InvestbookCashTable(getReport());
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new InvestbookCashFlowTable(getReport());
    }

    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<SecurityTransaction> getSecurityTransactionTable() {
        Collection<SecurityTransaction> data = transactionTable.getData()
                .stream()
                .filter(t -> t instanceof SecurityTransaction)
                .map(t -> (SecurityTransaction) t)
                .collect(Collectors.toCollection(ArrayList::new));
        data.addAll(depositAndWithdrawalTable.getData());
        return WrappingReportTable.of(getReport(), data);
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        Collection<DerivativeTransaction> data = transactionTable.getData()
                .stream()
                .filter(t -> t instanceof DerivativeTransaction)
                .map(t -> (DerivativeTransaction) t)
                .toList();
        return WrappingReportTable.of(getReport(), data);
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        Collection<ForeignExchangeTransaction> data = transactionTable.getData()
                .stream()
                .filter(t -> t instanceof ForeignExchangeTransaction)
                .map(t -> (ForeignExchangeTransaction) t)
                .toList();
        return WrappingReportTable.of(getReport(), data);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getCouponAmortizationRedemptionTable() {
        Collection<SecurityEventCashFlow> data = securityEventCashFlowTable.getData()
                .stream()
                .filter(e -> e.getEventType() == COUPON || e.getEventType() == AMORTIZATION || e.getEventType() == REDEMPTION)
                .toList();
        // TODO include bond tax records
        return WrappingReportTable.of(getReport(), data);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getDividendTable() {
        Collection<SecurityEventCashFlow> data = securityEventCashFlowTable.getData()
                .stream()
                .filter(e -> e.getEventType() == DIVIDEND)
                .toList();
        // TODO include stock tax records
        return WrappingReportTable.of(getReport(), data);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        Collection<SecurityEventCashFlow> data = securityEventCashFlowTable.getData()
                .stream()
                .filter(e -> e.getEventType() == DERIVATIVE_PROFIT)
                .toList();
        return WrappingReportTable.of(getReport(), data);
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
