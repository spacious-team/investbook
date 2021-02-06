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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.PortfolioCash;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportParserService {
    private final InvestbookApiClient api;

    public void parse(ReportTables reportTables) {
        try {
            boolean isAdded = api.addPortfolio(Portfolio.builder()
                    .id(reportTables.getReport().getPortfolio())
                    .build());
            if (isAdded) {
                ReportTable<PortfolioCash> portfolioCashTable = reportTables.getCashTable();
                ReportTable<PortfolioProperty> portfolioPropertyTable = reportTables.getPortfolioPropertyTable();
                ReportTable<EventCashFlow> cashFlowTable = reportTables.getCashFlowTable();
                ReportTable<Security> portfolioSecuritiesTable = reportTables.getSecuritiesTable();
                ReportTable<SecurityTransaction> securityTransactionTable = reportTables.getSecurityTransactionTable();
                ReportTable<SecurityEventCashFlow> couponAndAmortizationTable = reportTables.getCouponAmortizationRedemptionTable();
                ReportTable<SecurityEventCashFlow> dividendTable = reportTables.getDividendTable();
                ReportTable<DerivativeTransaction> derivativeTransactionTable = reportTables.getDerivativeTransactionTable();
                ReportTable<SecurityEventCashFlow> derivativeCashFlowTable = reportTables.getDerivativeCashFlowTable();
                ReportTable<ForeignExchangeTransaction> fxTransactionTable = reportTables.getForeignExchangeTransactionTable();
                ReportTable<SecurityQuote> securityQuoteTable = reportTables.getSecurityQuoteTable();
                ReportTable<ForeignExchangeRate> foreignExchangeRateReportTable = reportTables.getForeignExchangeRateTable();

                portfolioPropertyTable.getData().forEach(api::addPortfolioProperty);
                api.addCashInfo(portfolioCashTable);
                portfolioSecuritiesTable.getData().forEach(api::addSecurity);
                cashFlowTable.getData().forEach(api::addEventCashFlow);
                securityTransactionTable.getData().forEach(api::addTransaction);
                couponAndAmortizationTable.getData().forEach(c -> {
                    if (api.addSecurity(c.getSecurity())) { // required for amortization
                        api.addSecurityEventCashFlow(c);
                    }
                });
                dividendTable.getData().forEach(api::addSecurityEventCashFlow);
                derivativeTransactionTable.getData().forEach(api::addTransaction);
                derivativeCashFlowTable.getData().forEach(c -> {
                    if (api.addSecurity(c.getSecurity())) {
                        if (c.getCount() == null &&
                                c.getEventType() == CashFlowType.DERIVATIVE_PROFIT) { // count is optional for derivatives
                            c = c.toBuilder().count(0).build();
                        }
                        api.addSecurityEventCashFlow(c);
                    }
                });
                fxTransactionTable.getData().forEach(api::addTransaction);
                securityQuoteTable.getData().forEach(api::addSecurityQuote);
                foreignExchangeRateReportTable.getData().forEach(api::addForeignExchangeRate);
            }
        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTables.getReport().getPath(), e);
            throw new RuntimeException(e);
        }
    }
}
