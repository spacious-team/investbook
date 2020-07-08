/*
 * InvestBook
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

package ru.investbook.parser.uralsib;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import ru.investbook.parser.ExcelTable;
import ru.investbook.pojo.CashFlowType;
import ru.investbook.pojo.Security;
import ru.investbook.pojo.SecurityEventCashFlow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static ru.investbook.parser.uralsib.PaymentsTable.PaymentsTableHeader.*;

@Slf4j
public class CouponAmortizationRedemptionTable extends PaymentsTable<SecurityEventCashFlow> {

    private static final BigDecimal minValue = BigDecimal.valueOf(0.01);
    private final List<Map.Entry<String, Instant>> redemptionDates;

    public CouponAmortizationRedemptionTable(UralsibBrokerReport report,
                                             PortfolioSecuritiesTable securitiesTable,
                                             SecurityTransactionTable securityTransactionTable) {
        super(report, securitiesTable, securityTransactionTable);
        this.redemptionDates = new SecurityRedemptionTable(report).getData();
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

        Security security = getSecurity(table, row, CashFlowType.AMORTIZATION);
        if (security == null) return emptyList();
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
                .currency(UralsibBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY)));
        Collection<SecurityEventCashFlow> data = new ArrayList<>();
        data.add(builder.build());

        BigDecimal tax = getTax(table, row);
        if (tax.abs().compareTo(minValue) >= 0) {
            data.add(builder.eventType(CashFlowType.TAX).value(tax).build());
        }
        return data;
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

    private BigDecimal getTax(ExcelTable table, Row row) {
        // информация о налоге по купонам облигаций не выводится в отчет брокера
        return BigDecimal.ZERO;
    }
}
