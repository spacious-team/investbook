/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.portfolio.portfolio.pojo.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportParserService {
    private final ReportTableStorage storage;

    public void parse(ReportTableFactory reportTableFactory) {
        try {
            boolean isAdded = storage.addPortfolio(Portfolio.builder()
                    .id(reportTableFactory.getReport().getPortfolio())
                    .build());
            if (isAdded) {
                ReportTable<PortfolioCash> portfolioCashTable = reportTableFactory.createPortfolioCashTable();
                ReportTable<PortfolioProperty> portfolioPropertyTable = reportTableFactory.getPortfolioPropertyTable();
                ReportTable<EventCashFlow> cashFlowTable = reportTableFactory.getCashFlowTable();
                ReportTable<Security> portfolioSecuritiesTable = reportTableFactory.getPortfolioSecuritiesTable();
                ReportTable<SecurityTransaction> transactionTable = reportTableFactory.getTransactionTable();
                ReportTable<SecurityEventCashFlow> couponAndAmortizationTable = reportTableFactory.getCouponAndAmortizationTable();
                ReportTable<SecurityEventCashFlow> dividendTable = reportTableFactory.getDividendTable();
                ReportTable<DerivativeTransaction> derivativeTransactionTable = reportTableFactory.getDerivativeTransactionTable();
                ReportTable<SecurityEventCashFlow> derivativeCashFlowTable = reportTableFactory.getDerivativeCashFlowTable();

                portfolioPropertyTable.getData().forEach(storage::addPortfolioProperty);
                storage.addCashInfo(portfolioCashTable);
                portfolioSecuritiesTable.getData().forEach(storage::addSecurity);
                cashFlowTable.getData().forEach(storage::addEventCashFlow);
                transactionTable.getData().forEach(storage::addTransaction);
                couponAndAmortizationTable.getData().forEach(c -> {
                    if (storage.addSecurity(c.getIsin())) { // required for amortization
                        storage.addSecurityEventCashFlow(c);
                    }
                });
                dividendTable.getData().forEach(storage::addSecurityEventCashFlow);
                derivativeTransactionTable.getData().forEach(storage::addTransaction);
                derivativeCashFlowTable.getData().forEach(c -> {
                    if (storage.addSecurity(c.getIsin())) {
                        storage.addSecurityEventCashFlow(c);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTableFactory.getReport().getPath(), e);
            throw new RuntimeException(e);
        }
    }
}
