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

import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.Assert;
import ru.investbook.parser.SingleAbstractReportTable;
import ru.investbook.parser.SingleBrokerReport;

import java.util.Collection;

import static org.springframework.util.StringUtils.hasLength;
import static ru.investbook.parser.tinkoff.TinkoffCashFlowTable.CashFlowTableHeader.*;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.declareSecurity;
import static ru.investbook.parser.tinkoff.TinkoffSecurityTransactionTableHelper.getSecurity;

public class TinkoffSecurityEventCashFlowTable extends SingleAbstractReportTable<SecurityEventCashFlow> {

    private final SecurityCodeAndIsinTable codeAndIsin;
    private String currency = null;

    protected TinkoffSecurityEventCashFlowTable(SingleBrokerReport report, SecurityCodeAndIsinTable codeAndIsin) {
        super(report,
                // Таблица не имеет собственного названия, поэтому ищем предыдущую таблицу,
                // строки чужой таблицы пропускаются
                (cell) -> cell.startsWith("2. Операции с денежными средствами"),
                (cell) -> TinkoffBrokerReport.tablesLastRowPattern.matcher(cell).lookingAt(),
                TinkoffCashFlowTable.CashFlowTableHeader.class);
        this.codeAndIsin = codeAndIsin;
    }

    @Override
    protected SecurityEventCashFlow parseRow(TableRow row) {
        currency = TinkoffCashFlowTableHelper.getCurrency(row, currency);
        if (currency == null) {
            return null;
        }
        String operation = row.getStringCellValueOrDefault(OPERATION, "").toLowerCase();
        if (!hasLength(operation) || operation.equalsIgnoreCase("Операция")) {
            return null;
        }

        if (operation.contains("выплата") && operation.contains("дивиденд")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.DIVIDEND)
                    .value(row.getBigDecimalCellValue(DEPOSIT))
                    .build();
        } else if (operation.contains("выплата") && operation.contains("купон")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.COUPON)
                    .value(row.getBigDecimalCellValue(DEPOSIT))
                    .build();
        } else if (operation.contains("налог") && operation.contains("дивиденд")) {
            return getBuilder(row, currency)
                    .eventType(CashFlowType.TAX)
                    .value(row.getBigDecimalCellValue(WITHDRAWAL).negate())
                    .build();
        } else if (operation.contains("налог") && operation.contains("купон")) { // предположение, нет примера
            return getBuilder(row, currency)
                    .eventType(CashFlowType.TAX)
                    .value(row.getBigDecimalCellValue(WITHDRAWAL).negate())
                    .build();
        }

        return null;
    }

    private SecurityEventCashFlow.SecurityEventCashFlowBuilder getBuilder(TableRow row, String currency) {
        return SecurityEventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .timestamp(getReport().convertToInstant(row.getStringCellValue(DATE)))
                .security(getSecurityId(row))
                .count(getCount(row))
                .currency(currency);
    }

    private int getSecurityId(TableRow row) {
        String description = row.getStringCellValue(DESCRIPTION);
        String shortName = description.split("/")[0].trim();
        String code = codeAndIsin.getCode(shortName);
        Security security = getSecurity(
                code,
                codeAndIsin,
                shortName,
                codeAndIsin.getSecurityType(code));
        return declareSecurity(security, getReport().getSecurityRegistrar());
    }

    private int getCount(TableRow row) {
        String description = row.getStringCellValue(DESCRIPTION);
        String text = description.split("/")[1].trim();
        Assert.isTrue(text.endsWith(" шт."), "Ожидается количество бумаг в формате '<Наименование>/ 10 шт.'");
        return Integer.parseInt(text.split("\s+")[0]);
    }

    @Override
    protected boolean checkEquality(SecurityEventCashFlow cash1, SecurityEventCashFlow cash2) {
        return SecurityEventCashFlow.checkEquality(cash1, cash2);
    }

    @Override
    protected Collection<SecurityEventCashFlow> mergeDuplicates(SecurityEventCashFlow oldObject,
                                                                SecurityEventCashFlow newObject) {
        return SecurityEventCashFlow.mergeDuplicates(oldObject, newObject);
    }
}
