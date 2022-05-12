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

package ru.investbook.parser.tinkoff;

import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.InitializableReportTable;
import org.spacious_team.broker.report_parser.api.ReportTable;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.report.ForeignExchangeRateService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_RUB;
import static org.spacious_team.broker.pojo.PortfolioPropertyType.TOTAL_ASSETS_USD;

public class TinkoffPortfolioPropertyTable extends InitializableReportTable<PortfolioProperty> {

    private final SingleBrokerReport report;
    private final ForeignExchangeRateService foreignExchangeRateService;
    private final LocalDate date;
    private final ReportTable<PortfolioCash> tinkoffCashTable;
    private final TinkoffSecurityQuoteTable[] tinkoffSecurityQuoteTable;

    public TinkoffPortfolioPropertyTable(SingleBrokerReport report,
                                         ForeignExchangeRateService foreignExchangeRateService,
                                         ReportTable<PortfolioCash> tinkoffCashTable,
                                         TinkoffSecurityQuoteTable[] tinkoffSecurityQuoteTable) {
        super(report);
        this.report = report;
        this.foreignExchangeRateService = foreignExchangeRateService;
        this.tinkoffCashTable = tinkoffCashTable;
        this.tinkoffSecurityQuoteTable = tinkoffSecurityQuoteTable;
        this.date = LocalDate.ofInstant(report.getReportEndDateTime(), report.getReportZoneId());
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        Collection<PortfolioProperty> result = new ArrayList<>(2);

        getTotalAssets("RUB", TinkoffSecurityQuoteTable::getRubSecuritiesTotalValue)
                .map(value -> value.add(getNonRubAndNonUsdCashInRub()))
                .map(value -> toPortfolioProperty(TOTAL_ASSETS_RUB, value))
                .ifPresent(result::add);
        getTotalAssets("USD", TinkoffSecurityQuoteTable::getUsdSecuritiesTotalValue)
                .map(value -> toPortfolioProperty(TOTAL_ASSETS_USD, value))
                .ifPresent(result::add);

        return result;
    }

    private BigDecimal getNonRubAndNonUsdCashInRub() {
        return tinkoffCashTable.getData()
                .stream()
                .filter(cash -> !cash.getCurrency().equalsIgnoreCase("RUB") &&
                        !cash.getCurrency().equalsIgnoreCase("USD") &&
                        Math.abs(cash.getValue().floatValue()) > 1e-3)
                .map(cash -> cash.getValue().multiply(foreignExchangeRateService
                                .getExchangeRateOrDefault(cash.getCurrency(), "RUB", date)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<BigDecimal> getTotalAssets(String currency,
                                                Function<TinkoffSecurityQuoteTable, BigDecimal> securityValueEstimateFunction) {
        BigDecimal securityValueEstimate = Stream.of(tinkoffSecurityQuoteTable)
                .map(securityValueEstimateFunction)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return tinkoffCashTable.getData()
                .stream()
                .filter(cash -> cash.getCurrency().equalsIgnoreCase(currency))
                .map(PortfolioCash::getValue)
                .map(value -> value.add(securityValueEstimate))
                .findAny();
    }

    private PortfolioProperty toPortfolioProperty(PortfolioPropertyType type, BigDecimal value) {
        return PortfolioProperty.builder()
                .portfolio(report.getPortfolio())
                .timestamp(report.getReportEndDateTime())
                .property(type)
                .value(String.valueOf(value))
                .build();
    }
}
