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
import lombok.RequiredArgsConstructor;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

@RequiredArgsConstructor
public class UralsibReportTableFactory implements ReportTableFactory {
    @Getter
    private final UralsibBrokerReport report;

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
        return new EmptyReportTable<>(report);
    }
    
    @Override
    public ReportTable<SecurityTransaction> getTransactionTable() {
        return new EmptyReportTable<>(report);
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return new EmptyReportTable<>(report);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getCouponAndAmortizationTable() {
        return new EmptyReportTable<>(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDividendTable() {
        return new EmptyReportTable<>(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        return new EmptyReportTable<>(report);
    }
}
