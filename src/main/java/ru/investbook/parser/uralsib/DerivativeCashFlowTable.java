/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends SingleAbstractReportTable<SecurityEventCashFlow> {

    private final Pattern contractPattern = Pattern.compile(".*\\sвариационной маржи по\\s(.+)$");

    public DerivativeCashFlowTable(UralsibBrokerReport report) {
        super(report, PaymentsTable.TABLE_NAME, "", PaymentsTable.PaymentsTableHeader.class);
    }

    protected SecurityEventCashFlow parseRow(TableRow row) {
        String action = row.getStringCellValue(OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        if (!action.equalsIgnoreCase("вариационная маржа")) {
            return null;
        }
        return SecurityEventCashFlow.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .portfolio(getReport().getPortfolio())
                .value(row.getBigDecimalCellValue(VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(row.getStringCellValue(CURRENCY)))
                .eventType(CashFlowType.DERIVATIVE_PROFIT)
                .security(getContract(row))
                .build();
    }

    private String getContract(TableRow row) {
        String description = row.getStringCellValueOrDefault(DESCRIPTION, "");
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
