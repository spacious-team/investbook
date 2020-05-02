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

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.AbstractReportTable;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.portfolio.portfolio.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends AbstractReportTable<SecurityEventCashFlow> {

    private final Pattern contractPattern = Pattern.compile(".*\\sвариационной маржи по\\s(.+)$");

    public DerivativeCashFlowTable(UralsibBrokerReport report) {
        super(report, PaymentsTable.TABLE_NAME, "", PaymentsTable.PaymentsTableHeader.class);
    }

    protected Collection<SecurityEventCashFlow> getRow(ExcelTable table, Row row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        if (!action.equalsIgnoreCase("вариационная маржа")) {
            return emptyList();
        }
        return singletonList(SecurityEventCashFlow.builder()
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .portfolio(getReport().getPortfolio())
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .eventType(CashFlowType.DERIVATIVE_PROFIT)
                .isin(getContract(table, row))
                .build());
    }

    private String getContract(ExcelTable table, Row row) {
        String description = table.getStringCellValue(row, DESCRIPTION);
        Matcher matcher = contractPattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("Не могу найти наименование контракта в отчете брокера по событию:" + description);
    }

    @Override
    protected boolean checkEquality(SecurityEventCashFlow cash1, SecurityEventCashFlow cash2) {
        return SecurityEventCashFlow.checkEquality(cash1, cash2);
    }

    @Override
    protected Collection<SecurityEventCashFlow> mergeDuplicates(SecurityEventCashFlow oldObject, SecurityEventCashFlow newObject) {
        return SecurityEventCashFlow.mergeDuplicates(oldObject, newObject);
    }
}
