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

package ru.portfolio.portfolio.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.portfolio.portfolio.parser.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static ru.portfolio.portfolio.parser.psb.TransactionTable.TransactionTableHeader.*;

@Slf4j
public class TransactionTable implements ReportTable<SecurityTransaction> {
    private static final String TABLE1_NAME = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами в дату заключения";
    private static final String TABLE2_NAME = "Сделки, совершенные с ЦБ на биржевых торговых площадках (Фондовый рынок) с расчетами Т+, рассчитанные в отчетном периоде";
    private static final String TABLE_END_TEXT = "Итого оборот";
    @Getter
    private final PsbBrokerReport report;
    @Getter
    private final List<SecurityTransaction> data = new ArrayList<>();

    public TransactionTable(PsbBrokerReport report) {
        this.report = report;
        this.data.addAll(parseTable(report, TABLE1_NAME));
        this.data.addAll(parseTable(report, TABLE2_NAME));
    }

    private List<SecurityTransaction> parseTable(PsbBrokerReport report, String tableName) {
        ExcelTable table = ExcelTable.of(report.getSheet(), tableName, TABLE_END_TEXT, TransactionTableHeader.class);
        return table.getDataCollection(report.getPath(), this::getTransaction);
    }

    private Collection<SecurityTransaction> getTransaction(ExcelTable table, org.apache.poi.ss.usermodel.Row row) {
        boolean isBuy = table.getStringCellValue(row, DIRECTION).equalsIgnoreCase("покупка");
        BigDecimal value = table.getCurrencyCellValue(row, VALUE);
        BigDecimal accruedInterest = table.getCurrencyCellValue(row, ACCRUED_INTEREST);
        if (isBuy) {
            value = value.negate();
            accruedInterest = accruedInterest.negate();
        }
        BigDecimal commission = table.getCurrencyCellValue(row, MARKET_COMMISSION)
                .add(table.getCurrencyCellValue(row, BROKER_COMMISSION))
                .add(table.getCurrencyCellValue(row, CLEARING_COMMISSION))
                .add(table.getCurrencyCellValue(row, ITS_COMMISSION))
                .negate();
        return Collections.singletonList(SecurityTransaction.builder()
                .timestamp(report.convertToInstant(table.getStringCellValue(row, DATE_TIME)))
                .transactionId(table.getLongCellValue(row, TRANSACTION))
                .portfolio(getReport().getPortfolio())
                .isin(table.getStringCellValue(row, ISIN))
                .count((isBuy ? 1 : -1) * table.getIntCellValue(row, COUNT))
                .value(value)
                .accruedInterest(accruedInterest)
                .commission(commission)
                .valueCurrency(table.getStringCellValue(row, VALUE_CURRENCY).replace(" ", "").split("/")[1])
                .commissionCurrency(table.getStringCellValue(row, COMMISSION_CURRENCY))
                .build());
    }

    enum TransactionTableHeader implements TableColumnDescription {
        DATE_TIME(TableColumnImpl.of("дата", "исполнения"), TableColumnImpl.of("дата и время")),
        TRANSACTION("номер сделки"),
        ISIN("isin"),
        DIRECTION("покупка", "продажа"),
        COUNT("кол-во"),
        VALUE("сумма сделки"),
        VALUE_CURRENCY("валюта сделки"),
        ACCRUED_INTEREST("^нкд$"),
        MARKET_COMMISSION("комиссия торговой системы"),
        CLEARING_COMMISSION("клиринговая комиссия"),
        ITS_COMMISSION("комиссия за итс"),
        BROKER_COMMISSION("ком", "брокера"),
        COMMISSION_CURRENCY("валюта", "брок", "комиссии");

        @Getter
        private final TableColumn column;
        TransactionTableHeader(String ... words) {
            this.column = TableColumnImpl.of(words);
        }

        TransactionTableHeader(TableColumn ... columns) {
            this.column = AnyOfTableColumn.of(columns);
        }
    }
}
