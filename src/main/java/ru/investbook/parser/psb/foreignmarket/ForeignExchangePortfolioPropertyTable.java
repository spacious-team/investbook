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

package ru.investbook.parser.psb.foreignmarket;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleBrokerReport;
import ru.investbook.parser.psb.ForeignExchangeRateTable;
import ru.investbook.parser.psb.PortfolioPropertyTable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.psb.PortfolioPropertyTable.SummaryTableHeader.*;

@Slf4j
public class ForeignExchangePortfolioPropertyTable extends PortfolioPropertyTable {

    static final String ASSETS = "Остаток средств на счете";

    public ForeignExchangePortfolioPropertyTable(SingleBrokerReport report) {
        super(report);
    }

    protected Table getSummaryTable() {
        return getSummaryTable(getReport(), ASSETS);
    }

    @Override
    protected Collection<PortfolioProperty> getTotalAssets(Table table) {
        try {
            @Nullable TableRow assetsRow = table.findRowByPrefix(ASSETS);
            @Nullable TableRow exchangeRateRow = table.findRowByPrefix(ForeignExchangeRateTable.EXCHANGE_RATE_ROW);
            if (assetsRow == null || exchangeRateRow == null) {
                return emptyList();
            }
            BigDecimal totalAssets = assetsRow.getBigDecimalCellValueOrDefault(RUB, BigDecimal.ZERO)
                    .add(assetsRow.getBigDecimalCellValueOrDefault(USD, BigDecimal.ZERO)
                            .multiply(exchangeRateRow.getBigDecimalCellValueOrDefault(USD, BigDecimal.ZERO)))
                    .add(assetsRow.getBigDecimalCellValueOrDefault(EUR, BigDecimal.ZERO)
                            .multiply(exchangeRateRow.getBigDecimalCellValueOrDefault(EUR, BigDecimal.ZERO)))
                    .add(assetsRow.getBigDecimalCellValueOrDefault(GBP, BigDecimal.ZERO)
                            .multiply(exchangeRateRow.getBigDecimalCellValueOrDefault(GBP, BigDecimal.ZERO)))
                    .add(assetsRow.getBigDecimalCellValueOrDefault(CHF, BigDecimal.ZERO)
                            .multiply(exchangeRateRow.getBigDecimalCellValueOrDefault(CHF, BigDecimal.ZERO)));
            return Collections.singletonList(PortfolioProperty.builder()
                    .portfolio(getReport().getPortfolio())
                    .property(PortfolioPropertyType.TOTAL_ASSETS_RUB)
                    .value(totalAssets.toString())
                    .timestamp(getReport().getReportEndDateTime())
                    .build());
        } catch (Exception e) {
            log.info("Не могу получить стоимость активов из отчета {}", getReport());
        }
        return emptyList();
    }
}
