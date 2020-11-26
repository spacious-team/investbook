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

package ru.investbook.parser.vtb;

import lombok.extern.slf4j.Slf4j;
import org.spacious_team.broker.pojo.CashFlowType;
import org.spacious_team.broker.pojo.EventCashFlow;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.spacious_team.broker.report_parser.api.AbstractReportTable;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.Table;
import org.spacious_team.table_wrapper.api.TableRow;
import org.spacious_team.table_wrapper.excel.ExcelTable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;
import static ru.investbook.parser.vtb.VtbCashFlowTable.TABLE_NAME;
import static ru.investbook.parser.vtb.VtbCashFlowTable.VtbCashFlowTableHeader.*;

@Slf4j
public class VtbCouponAmortizationRedemptionTable extends AbstractReportTable<SecurityEventCashFlow> {

    private static final Pattern couponPerOneBondPattern = Pattern.compile("размер куп. на 1 обл\\.\\s+([0-9.]+)");
    private static final Pattern amortizationPerOneBondPattern = Pattern.compile("ном\\.на 1 обл\\.\\s+([0-9.]+)");
    private static final Pattern[] registrationNumberPatterns = new Pattern[]{
            Pattern.compile("\\b([\\w]+-[\\w]+-[\\w]+-[\\w-]+)\\b"), // 3 or more dashed word
            Pattern.compile("\\b([\\w]+).\\s+размер куп"),
            Pattern.compile("\\b([\\w]+),\\s+частичное досроч")
    };
    private final SecurityRegNumberToIsinConverter regNumberToIsinConverter;
    private final VtbSecurityDepositAndWithdrawalTable vtbSecurityDepositAndWithdrawalTable;
    private final Collection<EventCashFlow> externalBondPayments = new ArrayList<>();

    protected VtbCouponAmortizationRedemptionTable(BrokerReport report,
                                                   SecurityRegNumberToIsinConverter regNumberToIsinConverter,
                                                   VtbSecurityDepositAndWithdrawalTable vtbSecurityDepositAndWithdrawalTable) {
        super(report, TABLE_NAME, null, VtbCashFlowTable.VtbCashFlowTableHeader.class);
        this.regNumberToIsinConverter = regNumberToIsinConverter;
        this.vtbSecurityDepositAndWithdrawalTable = vtbSecurityDepositAndWithdrawalTable;
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(Table table, TableRow row) {
        String operation = String.valueOf(table.getStringCellValueOrDefault(row, OPERATION, ""))
                .trim()
                .toLowerCase();
        String description = table.getStringCellValueOrDefault(row, DESCRIPTION, "");
        String lowercaseDescription = description.toLowerCase();
        CashFlowType eventType = null;
        switch (operation) {
            case "купонный доход":
                eventType = CashFlowType.COUPON;
                break;
            case "погашение ценных бумаг":
                if (lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч")) {
                    eventType = CashFlowType.AMORTIZATION;
                } else if (lowercaseDescription.contains("погаш. номин.ст-ти обл")) { // предположение
                    eventType = CashFlowType.REDEMPTION;
                }
                break;
            case "зачисление денежных средств":
                if (lowercaseDescription.contains("погаш. номин.ст-ти обл")) {
                    eventType = CashFlowType.REDEMPTION;
                } else if (lowercaseDescription.contains("част.погаш") || lowercaseDescription.contains("частичное досроч")) {
                    eventType = CashFlowType.AMORTIZATION;
                } else if (lowercaseDescription.contains("куп. дох. по обл")) {
                    eventType = CashFlowType.COUPON;
                }
                break;
        }
        if (eventType == null) {
            return Collections.emptyList();
        }
        BigDecimal tax = VtbDividendTable.getTax(lowercaseDescription);
        BigDecimal value = table.getCurrencyCellValue(row, VALUE)
                .add(tax.abs());
        Instant timestamp = ((ExcelTable) table).getDateCellValue(row, DATE).toInstant();
        String currency = VtbBrokerReport.convertToCurrency(table.getStringCellValue(row, CURRENCY));
        try {
            String isin = getIsin(lowercaseDescription, regNumberToIsinConverter);
            int count = switch (eventType) {
                case COUPON -> value.divide(getCouponPerOneBond(lowercaseDescription), 2, RoundingMode.HALF_UP)
                        .intValueExact();
                case AMORTIZATION -> value.divide(getAmortizationPerOneBond(lowercaseDescription), 2, RoundingMode.HALF_UP)
                        .intValueExact();
                case REDEMPTION -> vtbSecurityDepositAndWithdrawalTable.getBondRedemptionCount(isin)
                        .orElseThrow(() -> new IllegalArgumentException("Не удалось определить количество погашенных облигаций " + isin));
                default -> throw new UnsupportedOperationException();
            };
            SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                    .portfolio(getReport().getPortfolio())
                    .eventType(eventType)
                    .timestamp(timestamp)
                    .value(value)
                    .currency(currency)
                    .count(count)
                    .isin(isin);
            Collection<SecurityEventCashFlow> data = new ArrayList<>();
            data.add(builder.build());
            if (tax.abs().compareTo(minValue) >= 0) {
                data.add(builder
                        .eventType(CashFlowType.TAX)
                        .value(tax.negate())
                        .build());
            }
            return data;
        } catch (Exception e) {
            log.warn("Выплата будет сохранена без привязки к ISIN облигации: {}", description, e);
            addToExternalBonPayments(timestamp, eventType, value, tax, currency, description);
            return Collections.emptyList();
        }
    }

    private static String getIsin(String description, SecurityRegNumberToIsinConverter regNumberToIsinConverter) {
        String isin;
        for (Pattern pattern : registrationNumberPatterns) {
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                String regNumber = matcher.group(1);
                if ((isin = regNumberToIsinConverter.convertToIsin(regNumber)) != null) {
                    return isin;
                }
            }
        }
        throw new IllegalArgumentException("Не смог выделить ISIN облигации по ресгитрационному номеру: " + description);
    }

    private static BigDecimal getCouponPerOneBond(String description) {
        Matcher matcher = couponPerOneBondPattern.matcher(description);
        if (matcher.find()) {
            return BigDecimal.valueOf(parseDouble(matcher.group(1)));
        }
        throw new IllegalArgumentException("Не смогу выделить размер купона на одну облигацию из описания: " + description);
    }

    private static BigDecimal getAmortizationPerOneBond(String description) {
        Matcher matcher = amortizationPerOneBondPattern.matcher(description);
        if (matcher.find()) {
            return BigDecimal.valueOf(parseDouble(matcher.group(1)));
        }
        throw new IllegalArgumentException("Не смогу выделить размер купона на одну облигацию из описания: " + description);
    }

    private void addToExternalBonPayments(Instant timestamp,
                                          CashFlowType eventType,
                                          BigDecimal value,
                                          BigDecimal tax,
                                          String currency,
                                          String description) {
        EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(eventType)
                .timestamp(timestamp)
                .value(value)
                .currency(currency)
                .description(description);
        externalBondPayments.add(builder.build());
        if (tax.abs().compareTo(minValue) >= 0) {
            externalBondPayments.add(builder
                    .eventType(CashFlowType.TAX)
                    .value(tax.negate())
                    .build());
        }
    }

    public Collection<EventCashFlow> getExternalBondPayments() {
        initializeIfNeed();
        return externalBondPayments;
    }
}
