/*
 * InvestBook
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

package ru.investbook.parser.uralsib;

import lombok.Getter;
import ru.investbook.parser.*;
import ru.investbook.parser.uralsib.PortfolioSecuritiesTable.ReportSecurityInformation;
import ru.investbook.pojo.EventCashFlow;
import ru.investbook.pojo.Security;
import ru.investbook.pojo.SecurityEventCashFlow;
import ru.investbook.view.ForeignExchangeRateService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UralsibReportTableFactory implements ReportTableFactory {
    @Getter
    private final UralsibBrokerReport report;
    private final PortfolioSecuritiesTable portfolioSecuritiesTable;
    @Getter
    private final SecurityTransactionTable securityTransactionTable;
    @Getter
    private final PortfolioPropertyTable portfolioPropertyTable;
    @Getter
    private final CouponAmortizationRedemptionTable couponAmortizationRedemptionTable;
    @Getter
    private final DividendTable dividendTable;

    public UralsibReportTableFactory(UralsibBrokerReport report, ForeignExchangeRateService foreignExchangeRateService) {
        this.report = report;
        this.portfolioPropertyTable = new PortfolioPropertyTable(report, foreignExchangeRateService);
        this.portfolioSecuritiesTable = new PortfolioSecuritiesTable(report);
        this.securityTransactionTable = new SecurityTransactionTable(report, portfolioPropertyTable);
        this.couponAmortizationRedemptionTable =
                new CouponAmortizationRedemptionTable(report, portfolioSecuritiesTable, securityTransactionTable);
        this.dividendTable = new DividendTable(report, portfolioSecuritiesTable, securityTransactionTable);
    }

    @Override
    public ReportTable<PortfolioCash> createPortfolioCashTable() {
        return new PortfolioCashTable(report);
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
    public ReportTable<Security> getPortfolioSecuritiesTable() {
        return new WrappingReportTable<>(report, portfolioSecuritiesTable.getData()
                .stream()
                .map(ReportSecurityInformation::getSecurity)
                .collect(Collectors.toList()));
    }

    @Override
    public ReportTable<DerivativeTransaction> getDerivativeTransactionTable() {
        return new DerivativeTransactionTable(report);
    }

    @Override
    public ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable() {
        return new ForeignExchangeTransactionTable(report);
    }
    
    @Override
    public ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable() {
        return new DerivativeCashFlowTable(report);
    }
}
