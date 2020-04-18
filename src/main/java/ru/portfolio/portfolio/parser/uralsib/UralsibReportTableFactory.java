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

package ru.portfolio.portfolio.parser.uralsib;

import lombok.Getter;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.parser.uralsib.PortfolioSecuritiesTable.ReportSecurityInformation;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.util.stream.Collectors;

public class UralsibReportTableFactory implements ReportTableFactory {
    @Getter
    private final UralsibBrokerReport report;
    private PortfolioSecuritiesTable portfolioSecuritiesTable;
    private SecurityTransactionTable securityTransactionTable;

    public UralsibReportTableFactory(UralsibBrokerReport report) {
        this.report = report;
        this.portfolioSecuritiesTable = new PortfolioSecuritiesTable(report);
        this.securityTransactionTable = new SecurityTransactionTable(report);
    }

    @Override
    public ReportTable<PortfolioCash> createPortfolioCashTable() {
        return new PortfolioCashTable(report);
    }
        
    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new PortfolioPropertyTable(report);
    }
    
    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new CashFlowTable(report);
    }
    
    @Override
    public ReportTable<Security> getPortfolioSecuritiesTable() {
        return new WrappingReportTable<>(report, portfolioSecuritiesTable.getData()
                .stream()
                .map(ReportSecurityInformation::getSecurity)
                .collect(Collectors.toList()));
    }
    
    @Override
    public ReportTable<SecurityTransaction> getSecurityTransactionTable() {
        return securityTransactionTable;
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return new EmptyReportTable<>(report);
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return new ForeignExchangeTransactionTable(report);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getCouponAndAmortizationTable() {
        return new CouponAmortizationRedemptionTable(report, portfolioSecuritiesTable, securityTransactionTable);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDividendTable() {
        return new DividendTable(report, portfolioSecuritiesTable, securityTransactionTable);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        return new EmptyReportTable<>(report);
    }
}
