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
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.DerivativeTransaction;
import org.spacious_team.broker.report_parser.api.ForeignExchangeTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.spacious_team.broker.report_parser.api.SecurityTransaction;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static org.spacious_team.broker.pojo.CashFlowType.DERIVATIVE_PROFIT;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportParserService {
    private final InvestbookApiClient api;

    public void parse(ReportTables reportTables) {
        try {
            boolean isAdded = getPortfolios(reportTables).stream().allMatch(api::addPortfolio);
            if (!isAdded) {
                return;
            }

            ReportTable<PortfolioProperty> portfolioPropertyTable = reportTables.getPortfolioPropertyTable();
            ReportTable<PortfolioCash> portfolioCashTable = reportTables.getCashTable();
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
            api.addPortfolioCash(portfolioCashTable.getData());
            portfolioSecuritiesTable.getData().forEach(api::addSecurity);
            cashFlowTable.getData().forEach(api::addEventCashFlow);
            securityTransactionTable.getData().forEach(api::addTransaction);
            couponAndAmortizationTable.getData()
                    .stream()
                    .filter(c -> api.addSecurity(c.getSecurity())) // required for amortization
                    .forEach(api::addSecurityEventCashFlow);
            dividendTable.getData().forEach(api::addSecurityEventCashFlow);
            derivativeTransactionTable.getData().forEach(api::addTransaction);
            derivativeCashFlowTable.getData()
                    .stream()
                    .filter(c -> api.addSecurity(c.getSecurity()))
                    .map(ReportParserService::setDerivativeCashFlowDefaults)
                    .forEach(api::addSecurityEventCashFlow);
            fxTransactionTable.getData().forEach(api::addTransaction);
            securityQuoteTable.getData().forEach(api::addSecurityQuote);
            foreignExchangeRateReportTable.getData().forEach(api::addForeignExchangeRate);

        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTables.getReport(), e);
            throw new RuntimeException(e);
        }
    }

    private static Set<Portfolio> getPortfolios(ReportTables tables) {
        Set<String> portfolios = new HashSet<>();

        addPortfolios(portfolios, tables.getPortfolioPropertyTable(), PortfolioProperty::getPortfolio);
        addPortfolios(portfolios, tables.getCashTable(), PortfolioCash::getPortfolio);
        addPortfolios(portfolios, tables.getCashFlowTable(), EventCashFlow::getPortfolio);

        addPortfolios(portfolios, tables.getSecurityTransactionTable(), SecurityTransaction::getPortfolio);
        addPortfolios(portfolios, tables.getDerivativeTransactionTable(), DerivativeTransaction::getPortfolio);
        addPortfolios(portfolios, tables.getForeignExchangeTransactionTable(), ForeignExchangeTransaction::getPortfolio);

        addPortfolios(portfolios, tables.getCouponAmortizationRedemptionTable(), SecurityEventCashFlow::getPortfolio);
        addPortfolios(portfolios, tables.getDividendTable(), SecurityEventCashFlow::getPortfolio);
        addPortfolios(portfolios, tables.getDerivativeCashFlowTable(), SecurityEventCashFlow::getPortfolio);

        return portfolios.stream()
                .map(portfolio -> Portfolio.builder().id(portfolio).build())
                .collect(Collectors.toSet());
    }

    private static <T> void addPortfolios(Collection<String> dest,
                                          ReportTable<T> fromTable, Function<T, String> toPortfolio) {
        fromTable.getData()
                .stream()
                .map(toPortfolio)
                .collect(toCollection(() -> dest));
    }

    private static SecurityEventCashFlow setDerivativeCashFlowDefaults(SecurityEventCashFlow c) {
        if (c.getCount() == null && c.getEventType() == DERIVATIVE_PROFIT) {
            // count is optional for derivatives
            c = c.toBuilder()
                    .count(0)
                    .build();
        }
        return c;
    }
}
