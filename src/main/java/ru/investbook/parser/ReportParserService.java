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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.*;
import org.spacious_team.broker.report_parser.api.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportParserService {
    private final ReportTableStorage storage;

    public void parse(ReportTables reportTables) {
        try {
            boolean isAdded = storage.addPortfolio(Portfolio.builder()
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

                portfolioPropertyTable.getData().forEach(storage::addPortfolioProperty);
                storage.addCashInfo(portfolioCashTable);
                portfolioSecuritiesTable.getData().forEach(storage::addSecurity);
                cashFlowTable.getData().forEach(storage::addEventCashFlow);
                securityTransactionTable.getData().forEach(storage::addTransaction);
                couponAndAmortizationTable.getData().forEach(c -> {
                    if (storage.addSecurity(c.getIsin())) { // required for amortization
                        storage.addSecurityEventCashFlow(c);
                    }
                });
                dividendTable.getData().forEach(storage::addSecurityEventCashFlow);
                derivativeTransactionTable.getData().forEach(storage::addTransaction);
                derivativeCashFlowTable.getData().forEach(c -> {
                    if (storage.addSecurity(c.getIsin())) {
                        if (c.getCount() == null &&
                                c.getEventType() == CashFlowType.DERIVATIVE_PROFIT) { // count is optional for derivatives
                            c = c.toBuilder().count(0).build();
                        }
                        storage.addSecurityEventCashFlow(c);
                    }
                });
                fxTransactionTable.getData().forEach(storage::addTransaction);
                securityQuoteTable.getData().forEach(storage::addSecurityQuote);
            }
        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTables.getReport().getPath(), e);
            throw new RuntimeException(e);
        }
    }
}
