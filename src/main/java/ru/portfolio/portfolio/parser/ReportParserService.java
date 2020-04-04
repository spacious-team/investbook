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
    private final ReportTableSaver saver;

    public void parse(ReportTableFactory reportTableFactory) {
        try {
            boolean isAdded = saver.addPortfolio(Portfolio.builder()
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

                portfolioPropertyTable.getData().forEach(saver::addPortfolioProperty);
                saver.addCashInfo(portfolioCashTable);
                portfolioSecuritiesTable.getData().forEach(saver::addSecurity);
                cashFlowTable.getData().forEach(saver::addEventCashFlow);
                transactionTable.getData().forEach(saver::addTransaction);
                couponAndAmortizationTable.getData().forEach(c -> {
                    if (saver.addSecurity(c.getIsin())) { // required for amortization
                        saver.addSecurityEventCashFlow(c);
                    }
                });
                dividendTable.getData().forEach(saver::addSecurityEventCashFlow);
                derivativeTransactionTable.getData().forEach(saver::addTransaction);
                derivativeCashFlowTable.getData().forEach(c -> {
                    if (saver.addSecurity(c.getIsin())) {
                        saver.addSecurityEventCashFlow(c);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTableFactory.getReport().getPath(), e);
            throw new RuntimeException(e);
        }
    }
}
