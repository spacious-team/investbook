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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.portfolio.portfolio.parser.BrokerReport;
import ru.portfolio.portfolio.parser.ExcelTable;
import ru.portfolio.portfolio.parser.ReportTable;
import ru.portfolio.portfolio.parser.SecurityTransaction;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.Security;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static ru.portfolio.portfolio.parser.uralsib.CashFlowTable.CashFlowTableHeader.*;

@Slf4j
public class CouponAndAmortizationTable implements ReportTable<SecurityEventCashFlow> {

    @Getter
    private final BrokerReport report;
    @Getter
    private final List<SecurityEventCashFlow> data = new ArrayList<>();
    // human readable name -> incoming count
    private final List<Map.Entry<Security, Integer>> securitiesIncomingCount;
    private final List<SecurityTransaction> securityTransactions;
    private final List<Map.Entry<String, Instant>> redemptionDates;

    public CouponAndAmortizationTable(UralsibBrokerReport report,
                                      PortfolioSecuritiesTable securitiesTable,
                                      SecurityTransactionTable securityTransactionTable) {
        this.report = report;
        this.securitiesIncomingCount = securitiesTable.getData();
        this.securityTransactions = securityTransactionTable.getData();
        this.redemptionDates = new SecurityRedemptionTable(report).getData();
        ExcelTable table = ExcelTable.of(report.getSheet(), CashFlowTable.TABLE_NAME, CashFlowTable.CashFlowTableHeader.class);
        data.addAll(pasreTable(table));
    }

    protected Collection<SecurityEventCashFlow> pasreTable(ExcelTable table) {
        return table.getDataCollection(getReport().getPath(), this::getRow);
    }

    protected Collection<SecurityEventCashFlow> getRow(ExcelTable table, Row row) {
        CashFlowType event;
        String action = table.getStringCellValue(row, OPERATION);
        action = String.valueOf(action).toLowerCase().trim();
        if (action.equalsIgnoreCase("погашение купона")) {
            event = CashFlowType.COUPON;
        } else if (action.equalsIgnoreCase("погашение номинала")) { // и амортизация и погашение
            event = null;
        } else {
            return emptyList();
        }

        String description = table.getStringCellValue(row, DESCRIPTION);
        Security security = getSecurity(description);
        Instant timestamp = getReport().convertToInstant(table.getStringCellValue(row, DATE));

        if (event == null) {
            if (isRedemption(security.getName(), timestamp)) {
                event = CashFlowType.REDEMPTION;
            } else {
                event = CashFlowType.AMORTIZATION;
            }
        }

        SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                .isin(security.getIsin())
                .portfolio(getReport().getPortfolio())
                .count(getSecurityCount(security, timestamp))
                .eventType(event)
                .timestamp(timestamp)
                .value(table.getCurrencyCellValue(row, VALUE))
                .currency(table.getStringCellValue(row, CURRENCY));
        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());

        BigDecimal tax = getTax(description);
        if (!tax.equals(BigDecimal.ZERO)) {
            data.add(builder.eventType(CashFlowType.TAX).value(tax).build());
        }
        return data;
    }

    private Security getSecurity(String eventDescription) {
        String eventDescriptionLowercase = eventDescription.toLowerCase();
        for (Map.Entry<Security, Integer> e : securitiesIncomingCount) {
            Security security = e.getKey();
            String securityName = security.getName();
            if (securityName != null && eventDescriptionLowercase.contains(securityName.toLowerCase())) {
                return security;
            }
        }
        throw new RuntimeException("Не могу найти ISIN ценной бумаги в отчете брокера по событию:" + eventDescription);
    }

    private boolean isRedemption(String securityName, Instant amortizationDay) {
        LocalDate redemptionDate = redemptionDates.stream()
                .filter(e -> securityName.equalsIgnoreCase(e.getKey()))
                .map(Map.Entry::getValue)
                .map(instant -> LocalDate.ofInstant(instant, UralsibBrokerReport.zoneId))
                .findAny()
                .orElse(null);
        return (redemptionDate != null) && redemptionDate.equals(LocalDate.ofInstant(amortizationDay, UralsibBrokerReport.zoneId));
    }

    private Integer getSecurityCount(Security security, Instant atInstant) {
        int count = securitiesIncomingCount.stream()
                .filter(e -> e.getKey().equals(security))
                .map(Map.Entry::getValue)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Не найдено количество на начало периода отчета для ЦБ " + security));
        Collection<SecurityTransaction> transactions = securityTransactions.stream()
                .filter(t -> t.getIsin().equals(security.getIsin()))
                .sorted(Comparator.comparing(SecurityTransaction::getTimestamp))
                .collect(Collectors.toList());
        for (SecurityTransaction transaction : transactions) {
            if (transaction.getTimestamp().isBefore(atInstant)) {
                count += transaction.getCount();
            } else {
                break;
            }
        }
        return count;
    }

    private BigDecimal getTax(String eventDescription) {
        // информация о налоге по купонам облигаций не выводится в отчет брокера
        return BigDecimal.ZERO;
    }
}
