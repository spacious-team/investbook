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
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.*;
import ru.portfolio.portfolio.pojo.PortfolioProperty;
import ru.portfolio.portfolio.pojo.PortfolioPropertyType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PortfolioPropertyTable.SummaryTableHeader.RUB;

public class PortfolioPropertyTable implements ReportTable<PortfolioProperty> {
    private static final String ASSETS_TABLE = "ОЦЕНКА АКТИВОВ";
    private static final String ASSETS = "Общая стоимость активов:";
    private static final BigDecimal min = BigDecimal.valueOf(0.01);
    @Getter
    private final BrokerReport report;
    @Getter
    private final List<PortfolioProperty> data = new ArrayList<>();


    protected PortfolioPropertyTable(UralsibBrokerReport report) {
        this.report = report;
        this.data.addAll(getTotalAssets(report));
        this.data.addAll(getExchangeRate(report));
    }

    protected static Collection<PortfolioProperty> getTotalAssets(BrokerReport report) {
        ExcelTable table = ExcelTable.of(report.getSheet(), ASSETS_TABLE, SummaryTableHeader.class);
        if (table.isEmpty()) {
            throw new IllegalArgumentException("Таблица '" + ASSETS_TABLE + "' не найдена");
        }
        Row row = table.findRow(ASSETS);
        if (row == null) {
            return emptyList();
        }
        return Collections.singletonList(PortfolioProperty.builder()
                .portfolio(report.getPortfolio())
                .property(PortfolioPropertyType.TOTAL_ASSETS)
                .value(table.getCurrencyCellValue(row, RUB).toString())
                .timestamp(report.getReportDate())
                .build());
    }

    protected static Collection<PortfolioProperty> getExchangeRate(BrokerReport report) {
        return singletonList(PortfolioProperty.builder()
                .portfolio(report.getPortfolio())
                .property(PortfolioPropertyType.USD_EXCHANGE_RATE)
                .value(getUsdExchangeRate(report).toString())
                .timestamp(report.getReportDate())
                .build());
    }

    private static BigDecimal getUsdExchangeRate(BrokerReport report) {
        return BigDecimal.valueOf(
                Double.parseDouble(
                        report.getSheet()
                                .getRow(12)
                                .getCell(0)
                                .getStringCellValue()
                                .split(" ")[2]
                                .replace(",", ".")));
    }

    enum SummaryTableHeader implements TableColumnDescription {
        RUB(13);

        @Getter
        private final TableColumn column;
        SummaryTableHeader(int columnIndex) {
            this.column = ConstantPositionTableColumn.of(columnIndex);
        }
    }
}
