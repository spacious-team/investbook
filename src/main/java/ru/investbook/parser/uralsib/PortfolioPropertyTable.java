/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.uralsib;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioCash;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import ru.investbook.parser.SingleInitializableReportTable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.report.ForeignExchangeRateService.RUB;

@Slf4j
public class PortfolioPropertyTable extends SingleInitializableReportTable<PortfolioProperty> {
    private final AssetsTable securityAssetsTable;
    private final CashTable cashTable;
    private final ForeignExchangeRateTable foreignExchangeRateTable;

    public PortfolioPropertyTable(AssetsTable securityAssetsTable, CashTable cashTable,
                                  ForeignExchangeRateTable foreignExchangeRateTable) {
        super(securityAssetsTable.getReport());
        this.securityAssetsTable = securityAssetsTable;
        this.cashTable = cashTable;
        this.foreignExchangeRateTable = foreignExchangeRateTable;
    }

    @Override
    protected Collection<PortfolioProperty> parseTable() {
        try {
            Collection<PortfolioProperty> securityAssets = securityAssetsTable.getData();
            if (!securityAssets.isEmpty()) {
                return securityAssets;
            }

            BigDecimal assets = BigDecimal.ZERO;
            Instant reportEndDateTime = getReport().getReportEndDateTime();
            for (PortfolioCash cash : cashTable.getData()) {
                BigDecimal value = cash.getValue();
                if (value.floatValue() > 0.001f) {
                    String currency = cash.getCurrency();
                    BigDecimal exchangeRate = foreignExchangeRateTable.getExchangeRate(currency, RUB, reportEndDateTime);
                    assets = assets.add(value.multiply(exchangeRate));
                }
            }

            return singletonList(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .timestamp(reportEndDateTime)
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                    .value(assets.toString())
                    .build());
        } catch (Exception e) {
            log.warn("Не могу получить активы из отчета {}", getReport(), e);
            return emptyList();
        }
    }
}
