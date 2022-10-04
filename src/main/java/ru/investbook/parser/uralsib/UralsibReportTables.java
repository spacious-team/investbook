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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.parser.TransactionValueAndFeeParser;
import ru.investbook.parser.uralsib.SecuritiesTable.ReportSecurityInformation;
import ru.investbook.report.ForeignExchangeRateService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UralsibReportTables extends AbstractReportTables<UralsibBrokerReport> {

    @Getter
    private final CashTable portfolioCashTable;
    private final SecuritiesTable portfolioSecuritiesTable;
    private final ReportTable<SecurityTransaction> securityTransactionTable;
    @Getter
    private final PortfolioPropertyTable portfolioPropertyTable;
    @Getter
    private final ForeignExchangeRateTable foreignExchangeRateTable;
    private final CouponAmortizationRedemptionTable couponAmortizationRedemptionTable;
    private final DividendTable dividendTable;
    @Getter
    private final ReportTable<SecurityQuote> securityQuoteTable;

    public UralsibReportTables(UralsibBrokerReport report,
                               ForeignExchangeRateService foreignExchangeRateService,
                               TransactionValueAndFeeParser transactionValueAndFeeParser) {
        super(report);
        AssetsTable securityAssetsTable = new AssetsTable(report);
        this.portfolioCashTable = new CashTable(report);
        this.foreignExchangeRateTable = new ForeignExchangeRateTable(report, foreignExchangeRateService);
        this.portfolioPropertyTable = new PortfolioPropertyTable(securityAssetsTable, portfolioCashTable, foreignExchangeRateTable);
        this.portfolioSecuritiesTable = new SecuritiesTable(report);
        this.securityTransactionTable = WrappingReportTable.of(
                new SecurityTransactionTable(report, foreignExchangeRateTable, transactionValueAndFeeParser),
                new SecurityDepositAndWithdrawalTable(report, portfolioSecuritiesTable));
        this.couponAmortizationRedemptionTable =
                new CouponAmortizationRedemptionTable(report, portfolioSecuritiesTable, securityTransactionTable);
        this.dividendTable = new DividendTable(report, portfolioSecuritiesTable, securityTransactionTable);
        this.securityQuoteTable = WrappingReportTable.of(
                new SecurityQuoteTable(report, foreignExchangeRateTable),
                new DerivativeQuoteTable(report));
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        List<EventCashFlow> data = new ArrayList<>();
        data.addAll(new CashFlowTable(report).getData());
        data.addAll(couponAmortizationRedemptionTable.getEventCashFlows());
        data.addAll(dividendTable.getEventCashFlows());
        return WrappingReportTable.of(report, data);
    }

    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return WrappingReportTable.of(report, portfolioSecuritiesTable.getData()
                .stream()
                .map(ReportSecurityInformation::getSecurity)
                .collect(Collectors.toList()));
    }

    @Override
    public ReportTable<AbstractTransaction> getTransactionTable() {
        return WrappingReportTable.of(
                securityTransactionTable,
                new DerivativeTransactionTable(report),
                new DerivativeExpirationTable(report),
                new ForeignExchangeTransactionTable(report));
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getSecurityEventCashFlowTable() {
        return WrappingReportTable.of(
                couponAmortizationRedemptionTable,
                dividendTable,
                new DerivativeCashFlowTable(report));
    }
}
