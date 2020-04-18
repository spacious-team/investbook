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

import ru.portfolio.portfolio.pojo.EventCashFlow;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

public interface ReportTableFactory {
    BrokerReport getReport();

    ReportTable<PortfolioCash> createPortfolioCashTable();

    ReportTable<PortfolioProperty> getPortfolioPropertyTable();

    ReportTable<EventCashFlow> getCashFlowTable();

    ReportTable<Security> getPortfolioSecuritiesTable();

    ReportTable<SecurityTransaction> getSecurityTransactionTable();

    ReportTable<DerivativeTransaction> getDerivativeTransactionTable();

    ReportTable<ForeignExchangeTransaction> getForeignExchangeTransactionTable();

    ReportTable<SecurityEventCashFlow> getCouponAndAmortizationTable();

    ReportTable<SecurityEventCashFlow> getDividendTable();

    ReportTable<SecurityEventCashFlow> getDerivativeCashFlowTable();
}
