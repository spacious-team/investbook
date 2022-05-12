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

package ru.investbook.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.Portfolio;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractTransaction;
import org.spacious_team.broker.report_parser.api.ReportTable;
import org.spacious_team.broker.report_parser.api.ReportTables;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;

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

            reportTables.getPortfolioPropertyTable()
                    .getData()
                    .forEach(api::addPortfolioProperty);
            reportTables.getPortfolioCashTable()
                    .getData()
                    .forEach(api::addPortfolioCash);
            reportTables.getSecuritiesTable()
                    .getData()
                    .forEach(api::addSecurity);
            reportTables.getCashFlowTable()
                    .getData()
                    .forEach(api::addEventCashFlow);
            reportTables.getTransactionTable()
                    .getData()
                    .forEach(api::addTransaction);
            reportTables.getSecurityEventCashFlowTable()
                    .getData()
                    .forEach(api::addSecurityEventCashFlow);
            reportTables.getSecurityQuoteTable()
                    .getData()
                    .forEach(api::addSecurityQuote);
            reportTables.getForeignExchangeRateTable()
                    .getData()
                    .forEach(api::addForeignExchangeRate);

        } catch (Exception e) {
            log.warn("Не могу распарсить отчет {}", reportTables.getReport(), e);
            throw new RuntimeException(e);
        }
    }

    private static Set<Portfolio> getPortfolios(ReportTables tables) {
        Set<String> portfolios = new HashSet<>();

        addPortfolios(portfolios, tables.getPortfolioPropertyTable(), PortfolioProperty::getPortfolio);
        addPortfolios(portfolios, tables.getPortfolioCashTable(), PortfolioCash::getPortfolio);
        addPortfolios(portfolios, tables.getCashFlowTable(), EventCashFlow::getPortfolio);
        addPortfolios(portfolios, tables.getTransactionTable(), AbstractTransaction::getPortfolio);
        addPortfolios(portfolios, tables.getSecurityEventCashFlowTable(), SecurityEventCashFlow::getPortfolio);

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
}
