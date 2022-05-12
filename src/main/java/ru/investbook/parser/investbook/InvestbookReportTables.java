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

package ru.investbook.parser.investbook;

import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.converter.SecurityConverter;
import ru.investbook.parser.SecurityRegistrar;
import ru.investbook.repository.SecurityRepository;

public class InvestbookReportTables extends AbstractReportTables<BrokerReport> {

    private final SecurityRegistrar securityRegistrar;
    private final SecurityRepository securityRepository;
    private final SecurityConverter securityConverter;

    protected InvestbookReportTables(BrokerReport report,
                                     SecurityRegistrar securityRegistrar,
                                     SecurityRepository securityRepository,
                                     SecurityConverter securityConverter) {
        super(report);
        this.securityRegistrar = securityRegistrar;
        this.securityRepository = securityRepository;
        this.securityConverter = securityConverter;
    }

    @Override
    public ReportTable<PortfolioProperty> getPortfolioPropertyTable() {
        return new InvestbookPortfolioPropertyTable(getReport());
    }

    @Override
    public ReportTable<PortfolioCash> getPortfolioCashTable() {
        return new InvestbookPortfolioCashTable(getReport());
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new InvestbookCashFlowTable(getReport());
    }

    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<AbstractTransaction> getTransactionTable() {
        return WrappingReportTable.of(
                new InvestbookTransactionTable(
                        getReport(), securityRegistrar, securityRepository, securityConverter),
                new InvestbookSecurityDepositAndWithdrawalTable(
                        getReport(), securityRegistrar, securityRepository, securityConverter));
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getSecurityEventCashFlowTable() {
        return new InvestbookSecurityEventCashFowTable(
                getReport(), securityRegistrar, securityRepository, securityConverter);
    }

    @Override
    public ReportTable<SecurityQuote> getSecurityQuoteTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<ForeignExchangeRate> getForeignExchangeRateTable() {
        return emptyTable();
    }
}
