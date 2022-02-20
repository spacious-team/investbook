/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Double.parseDouble;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static ru.investbook.parser.vtb.VtbBrokerReport.minValue;

@Slf4j
public class VtbCouponAmortizationRedemptionTable extends AbstractVtbCashFlowTable<SecurityEventCashFlow> {

    private static final Pattern couponPerOneBondPattern = Pattern.compile("размер куп. на 1 обл\\.\\s+([0-9.]+)");
    private static final Pattern amortizationPerOneBondPattern = Pattern.compile("ном\\.на 1 обл\\.\\s+([0-9.]+)");
    private static final Pattern[] registrationNumberPatterns = new Pattern[]{
            Pattern.compile("\\b([\\w]+-[\\w]+-[\\w]+-[\\w-]+)\\b"), // 3 or more dashed word
            Pattern.compile("\\b([\\w]+).\\s+размер куп"),
            Pattern.compile("\\b([\\w]+),\\s+частичное досроч")
    };
    private final SecurityRegNumberRegistrar securityRegNumberRegistrar;
    private final VtbSecurityDepositAndWithdrawalTable vtbSecurityDepositAndWithdrawalTable;
    private final Collection<EventCashFlow> externalBondPayments = new ArrayList<>();

    protected VtbCouponAmortizationRedemptionTable(CashFlowEventTable cashFlowEventTable,
                                                   SecurityRegNumberRegistrar securityRegNumberRegistrar,
                                                   VtbSecurityDepositAndWithdrawalTable vtbSecurityDepositAndWithdrawalTable) {
        super(cashFlowEventTable);
        this.securityRegNumberRegistrar = securityRegNumberRegistrar;
        this.vtbSecurityDepositAndWithdrawalTable = vtbSecurityDepositAndWithdrawalTable;
    }

    @Override
    protected Collection<SecurityEventCashFlow> getRow(CashFlowEventTable.CashFlowEvent event) {
        CashFlowType eventType = event.getEventType();
        if (eventType != COUPON && eventType != AMORTIZATION && eventType != REDEMPTION) {
            return Collections.emptyList();
        }
        String lowercaseDescription = event.getLowercaseDescription();
        BigDecimal tax = VtbDividendTable.getTax(lowercaseDescription);
        BigDecimal value = event.getValue()
                .add(tax.abs());
        try {
            Security security = getSecurity(lowercaseDescription);
            int count = switch (eventType) {
                case COUPON -> value.divide(getCouponPerOneBond(lowercaseDescription), 2, RoundingMode.HALF_UP)
                        .intValueExact();
                case AMORTIZATION -> value.divide(getAmortizationPerOneBond(lowercaseDescription), 2, RoundingMode.HALF_UP)
                        .intValueExact();
                case REDEMPTION -> vtbSecurityDepositAndWithdrawalTable.getBondRedemptionCount(security.getIsin())
                        .orElseThrow(() -> new IllegalArgumentException("Не удалось определить количество погашенных облигаций " + security.getIsin()));
                default -> throw new UnsupportedOperationException();
            };
            int securityId = getReport().getSecurityRegistrar().declareBondByIsin(security.getIsin(), security::toBuilder);
            SecurityEventCashFlow.SecurityEventCashFlowBuilder builder = SecurityEventCashFlow.builder()
                    .portfolio(getReport().getPortfolio())
                    .eventType(eventType)
                    .timestamp(event.getDate())
                    .value(value)
                    .currency(event.getCurrency())
                    .count(count)
                    .security(securityId);
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
            log.warn("Выплата будет сохранена без привязки к ISIN облигации: {}", event.getDescription(), e);
            addToExternalBondPayment(event, eventType, value, tax);
            return Collections.emptyList();
        }
    }

    private Security getSecurity(String description) {
        for (Pattern pattern : registrationNumberPatterns) {
            Matcher matcher = pattern.matcher(description);
            if (matcher.find()) {
                String regNumber = matcher.group(1);
                Optional<Security> security = securityRegNumberRegistrar.getSecurityByRegistrationNumber(regNumber);
                if (security.isPresent()) {
                    return security.get();
                }
            }
        }
        throw new IllegalArgumentException("Не смог выделить ISIN облигации по региcтрационному номеру: " + description);
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

    private void addToExternalBondPayment(CashFlowEventTable.CashFlowEvent event,
                                          CashFlowType eventType,
                                          BigDecimal value,
                                          BigDecimal tax) {
        String description = event.getDescription();
        EventCashFlow.EventCashFlowBuilder builder = EventCashFlow.builder()
                .portfolio(getReport().getPortfolio())
                .eventType(eventType)
                .timestamp(event.getDate())
                .value(value)
                .currency(event.getCurrency())
                .description(StringUtils.hasLength(description) ? description : null);
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
