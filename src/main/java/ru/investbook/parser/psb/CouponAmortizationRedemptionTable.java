/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.psb;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;

import static ru.investbook.parser.psb.CouponAmortizationRedemptionTable.CouponAndAmortizationTableHeader.*;

@Slf4j
public class CouponAmortizationRedemptionTable extends AbstractReportTable<SecurityEventCashFlow> {

    private static final String TABLE_NAME = "Погашение купонов и ЦБ";
    private static final String TABLE_END_TEXT = "*Налог удерживается с рублевого брокерского счета";
    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);

    public CouponAmortizationRedemptionTable(PsbBrokerReport report) {
        super(report, TABLE_NAME, TABLE_END_TEXT, CouponAndAmortizationTableHeader.class);
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(Table table, TableRow row) {
        CashFlowType event;
        String action = table.getStringCellValue(row, TYPE);
        if (action.equalsIgnoreCase("Погашение купона")) {
            event = CashFlowType.COUPON;
        } else if (action.equalsIgnoreCase("Амортизация")) {
            event = CashFlowType.AMORTIZATION;
        } else if (action.equalsIgnoreCase("Погашение бумаг")) {
            event = CashFlowType.REDEMPTION;
        } else {
            throw new RuntimeException("Обработчик события " + action + " не реализован");
        }

        BigDecimal value = ((event == CashFlowType.COUPON) ?
                table.getCurrencyCellValue(row, COUPON) :
                table.getCurrencyCellValue(row, VALUE));
        BigDecimal tax = table.getCurrencyCellValue(row, TAX).negate();

        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .isin(table.getStringCellValue(row, ISIN))
                .portfolio(getReport().getPortfolio())
                .count(table.getIntCellValue(row, COUNT))
                .eventType(event)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(value)
                .currency(table.getStringCellValue(row, CURRENCY));
        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());
        if (tax.abs().compareTo(minValue) >= 0) {
            data.add(builder.eventType(CashFlowType.TAX).value(tax).build());
        }
        return data;
    }

    enum CouponAndAmortizationTableHeader implements TableColumnDescription {
        DATE("дата"),
        TYPE("вид операции"),
        ISIN("isin"),
        COUNT("кол-во"),
        COUPON("нкд"),
        VALUE("сумма амортизации"),
        TAX("удержанного налога"),
        CURRENCY("валюта выплаты");

        @Getter
        private final TableColumn column;

        CouponAndAmortizationTableHeader(String... words) {
            this.column = TableColumnImpl.of(words);
        }
    }
}
