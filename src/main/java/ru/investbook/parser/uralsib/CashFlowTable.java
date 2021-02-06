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
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class CashFlowTable extends AbstractReportTable<EventCashFlow> {

    private final Pattern moneyTransferToDescriptionPattern = Pattern.compile(".*\\s+на\\s+[^\\s]+\\s+([^\\s.]+)");
    private final Pattern moneyTransferFromDescriptionPattern = Pattern.compile(".*\\s+с\\s+[^\\s]+\\s+([^\\s.]+)");
    private final Pattern clientCodePattern = Pattern.compile("(^[0-9]+)");

    public CashFlowTable(UralsibBrokerReport report) {
        super(report, PaymentsTable.TABLE_NAME, "", PaymentsTable.PaymentsTableHeader.class);
    }

    @Override
    protected Collection<EventCashFlow> getRow(Table table, TableRow row) {
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        String description = table.getStringCellValueOrDefault(row, DESCRIPTION, "");
        CashFlowType type;
        switch (action) {
            case"ввод дс":
            case"вывод дс":
                type = CashFlowType.CASH;
                break;
            case "перевод дс":
                Matcher matcherTo = moneyTransferToDescriptionPattern.matcher(description);
                Matcher matcherFrom = moneyTransferFromDescriptionPattern.matcher(description);
                if (matcherTo.find() && matcherFrom.find()) {
                    String to = matcherTo.group(1);
                    String from = matcherFrom.group(1);
                    if (isCurrentPortfolioAccount(to) != isCurrentPortfolioAccount(from)) {
                        type = CashFlowType.CASH;
                        break;
                    }
                }
                return emptyList();
            case "налог":
                type = CashFlowType.TAX;
                break;
            case "доначисление комиссии до размера минимальной":
            case "депозитарные сборы других депозитариев":
                type = CashFlowType.COMMISSION;
                break;
            default:
                return emptyList();
        }
        return singletonList(EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(type)
                .timestamp(convertToInstant(table.getStringCellValue(row, DATE)))
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)))
                .description(StringUtils.hasLength(description) ? description : null)
                .build());
    }

    private boolean isCurrentPortfolioAccount(String account) {
        String portfolio = getReport().getPortfolio();
        boolean isIIS = portfolio.endsWith("I");
        if (account.startsWith("SPBFUT")) {
            // срочный рынок
            return isIIS == (account.length() > 6 && account.charAt(6) == 'I');
        } else {
            // Мосбиржа, СПб биржа
            return getClientCode(portfolio).equals(getClientCode(account))
                    && (isIIS == account.endsWith("I"));
        }
    }

    private Integer getClientCode(String account) {
        try {
            Matcher matcher = clientCodePattern.matcher(account);
            matcher.find();
            return Integer.parseInt(matcher.group(1));
        } catch (Exception e) {
            throw new RuntimeException("Не могу найти код клиента для субсчета " + account);
        }
    }

    @Override
    protected boolean checkEquality(EventCashFlow flow1, EventCashFlow flow2) {
        return EventCashFlow.checkEquality(flow1, flow2);
    }

    @Override
    protected Collection<EventCashFlow> mergeDuplicates(EventCashFlow old, EventCashFlow nw) {
        return EventCashFlow.mergeDuplicates(old, nw);
    }
}
