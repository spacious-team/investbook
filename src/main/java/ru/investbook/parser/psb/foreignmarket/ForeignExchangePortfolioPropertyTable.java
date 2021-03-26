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

package ru.investbook.parser.psb.foreignmarket;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.PortfolioProperty;
import org.spacious_team.broker.pojo.PortfolioPropertyType;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
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

    public ForeignExchangePortfolioPropertyTable(BrokerReport report) {
        super(report);
    }

    protected Table getSummaryTable() {
        return getSummaryTable(getReport(), ASSETS);
    }

    @Override
    protected Collection<PortfolioProperty> getTotalAssets(Table table) {
        try {
            TableRow assetsRow = table.findRow(ASSETS);
            TableRow exchangeRateRow = table.findRow(ForeignExchangeRateTable.EXCHANGE_RATE_ROW);
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
            log.info("Не могу получить стоимость активов из отчета {}", getReport().getPath().getFileName());
        }
        return emptyList();
    }
}
