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

package ru.investbook.parser.uralsib;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableRow;
import ru.investbook.parser.SingleAbstractReportTable;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class DerivativeCashFlowTable extends SingleAbstractReportTable<SecurityEventCashFlow> {

    private final Pattern contractPattern = Pattern.compile(".*\\sвариационной маржи по\\s(.+)$");

    public DerivativeCashFlowTable(UralsibBrokerReport report) {
        super(report, PaymentsTable.TABLE_NAME, "", PaymentsTable.PaymentsTableHeader.class);
    }

    protected @Nullable SecurityEventCashFlow parseRow(TableRow row) {
        String action = row.getStringCellValue(OPERATION)
                .toLowerCase()
                .trim();
        if (!action.equalsIgnoreCase("вариационная маржа")) {
            return null;
        }
        return SecurityEventCashFlow.builder()
                .timestamp(convertToInstant(row.getStringCellValue(DATE)))
                .portfolio(getReport().getPortfolio())
                .value(row.getBigDecimalCellValue(VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(row.getStringCellValue(CURRENCY)))
                .eventType(CashFlowType.DERIVATIVE_PROFIT)
                .security(getSecurityId(row))
                .build();
    }

    private int getSecurityId(TableRow row) {
        @SuppressWarnings("DataFlowIssue")
        String description = row.getStringCellValueOrDefault(DESCRIPTION, "");
        @SuppressWarnings("DataFlowIssue")
        Matcher matcher = contractPattern.matcher(description);
        if (matcher.find()) {
            String code = requireNonNull(matcher.group(1));
            return getReport().getSecurityRegistrar().declareDerivative(code);
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
