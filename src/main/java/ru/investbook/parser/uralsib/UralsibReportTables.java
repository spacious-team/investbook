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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.parser.uralsib.SecuritiesTable.ReportSecurityInformation;
import ru.investbook.view.ForeignExchangeRateService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UralsibReportTables implements ReportTables {
    @Getter
    private final UralsibBrokerReport report;
    private final SecuritiesTable portfolioSecuritiesTable;
    @Getter
    private final ReportTable<SecurityTransaction> securityTransactionTable;
    @Getter
    private final PortfolioPropertyTable portfolioPropertyTable;
    @Getter
    private final CouponAmortizationRedemptionTable couponAmortizationRedemptionTable;
    @Getter
    private final DividendTable dividendTable;

    public UralsibReportTables(UralsibBrokerReport report, ForeignExchangeRateService foreignExchangeRateService) {
        this.report = report;
        this.portfolioPropertyTable = new PortfolioPropertyTable(report, foreignExchangeRateService);
        this.portfolioSecuritiesTable = new SecuritiesTable(report);
        this.securityTransactionTable = WrappingReportTable.of(
                new SecurityTransactionTable(report, portfolioPropertyTable),
                new SecurityDepositAndWithdrawalTable(report, portfolioSecuritiesTable));
        this.couponAmortizationRedemptionTable =
                new CouponAmortizationRedemptionTable(report, portfolioSecuritiesTable, securityTransactionTable);
        this.dividendTable = new DividendTable(report, portfolioSecuritiesTable, securityTransactionTable);
    }

    @Override
    public ReportTable<PortfolioCash> getCashTable() {
        return new CashTable(report);
    }
    
    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        List<EventCashFlow> data = new ArrayList<>();
        data.addAll(new CashFlowTable(report).getData());
        data.addAll(couponAmortizationRedemptionTable.getEventCashFlows());
        data.addAll(dividendTable.getEventCashFlows());
        return new WrappingReportTable<>(report, data);
    }
    
    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return new WrappingReportTable<>(report, portfolioSecuritiesTable.getData()
                .stream()
                .map(ReportSecurityInformation::getSecurity)
                .collect(Collectors.toList()));
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return WrappingReportTable.of(
                new DerivativeTransactionTable(report),
                new DerivativeExpirationTable(report));
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return new ForeignExchangeTransactionTable(report);
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
}
