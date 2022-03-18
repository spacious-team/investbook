/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.tinkoff;

import lombok.Getter;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.ForeignExchangeRate;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.pojo.SecurityQuote;
import org.spacious_team.broker.report_parser.api.AbstractReportTables;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.WrappingReportTable;
import ru.investbook.parser.TransactionValueAndFeeParser;
import ru.investbook.report.ForeignExchangeRateService;

public class TinkoffReportTables extends AbstractReportTables<TinkoffBrokerReport> {
    private final TransactionValueAndFeeParser transactionValueAndFeeParser;
    private final SecurityCodeAndIsinTable securityCodeAndIsinTable;
    @Getter
    private final ReportTable<SecurityQuote> securityQuoteTable;
    @Getter
    private final TinkoffCashTable portfolioCashTable;
    @Getter
    private final TinkoffPortfolioPropertyTable portfolioPropertyTable;

    protected TinkoffReportTables(TinkoffBrokerReport report,
                                  TransactionValueAndFeeParser transactionValueAndFeeParser,
                                    ForeignExchangeRateService foreignExchangeRateService) {
        super(report);
        this.transactionValueAndFeeParser = transactionValueAndFeeParser;
        this.securityCodeAndIsinTable = new SecurityCodeAndIsinTable(this.report);
        this.portfolioCashTable = new TinkoffCashTable(report);
        TinkoffSecurityQuoteTable[] securityQuoteTables =
                TinkoffSecurityQuoteTable.of(report, securityCodeAndIsinTable, foreignExchangeRateService);
        this.securityQuoteTable = WrappingReportTable.of(securityQuoteTables);
        this.portfolioPropertyTable = new TinkoffPortfolioPropertyTable(report, foreignExchangeRateService,
                portfolioCashTable, securityQuoteTables);
    }

    @Override
    public ReportTable<EventCashFlow> getCashFlowTable() {
        return new TinkoffCashFlowTable(report);
    }

    @Override
    public ReportTable<Security> getSecuritiesTable() {
        return emptyTable();
    }

    @Override
    public ReportTable<AbstractTransaction> getTransactionTable() {
        return TinkoffSecurityTransactionTable.of(report, securityCodeAndIsinTable, transactionValueAndFeeParser);
    }

    @Override
    public ReportTable<SecurityEventCashFlow> getSecurityEventCashFlowTable() {
        return new TinkoffSecurityEventCashFlowTable(report, securityCodeAndIsinTable);
    }

    @Override
    public ReportTable<ForeignExchangeRate> getForeignExchangeRateTable() {
        return emptyTable();
    }
}
