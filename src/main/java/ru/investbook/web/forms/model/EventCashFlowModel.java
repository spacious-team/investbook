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

package ru.investbook.web.forms.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Objects.requireNonNull;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static org.springframework.util.StringUtils.hasLength;

@Data
public class EventCashFlowModel {

    private @Nullable Integer id;

    private @NotEmpty String portfolio;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime time = LocalTime.NOON;

    private CashFlowType type;

    /**
     * Negative value for cash out, otherwise - positive
     */
    private BigDecimal value;

    private @NotEmpty String valueCurrency = "RUB";

    private @Nullable String description;

    /**
     * Используется для привязки выплаты к бумаге из того же или другого счета
     */
    private @Nullable AttachedSecurity attachedSecurity = new AttachedSecurity();

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    /**
     * Used by templates/events/edit-form.html
     */
    @SuppressWarnings("unused")
    public String getStringType() {
        return switch (type) {
            case DIVIDEND, COUPON, REDEMPTION, AMORTIZATION, ACCRUED_INTEREST -> type.name();
            case CASH -> {
                String desc = String.valueOf(description).toLowerCase();
                if (desc.contains("вычет") && desc.contains("иис")) {
                    yield "TAX_IIS_A";
                }
                yield type.name() + (isValuePositive() ? "_IN" : "_OUT");
            }
            default -> type.name() + (isValuePositive() ? "_IN" : "_OUT");
        };
    }

    /**
     * Used by templates/events/edit-form.html
     */
    @SuppressWarnings("unused")
    public void setStringType(String value) {
        if (value.equals("TAX_IIS_A")) {
            type = CashFlowType.CASH;
        } else {
            type = CashFlowType.valueOf(value.split("_")[0]);
        }
    }

    private boolean isValuePositive() {
        return value.compareTo(BigDecimal.ZERO) >= 0;
    }

    public boolean isAttachedToSecurity() {
        return attachedSecurity != null && attachedSecurity.isValid();
    }

    @Data
    public class AttachedSecurity {

        private @Nullable Integer securityEventCashFlowId;

        /**
         * In "name (isin)" or "contract-name" format
         */
        private @Nullable String security;

        private @Nullable @Positive Integer count;

        public boolean isValid() {
            return hasLength(security) &&
                    count != null && count > 0 &&
                    (type == DIVIDEND || type == COUPON || type == AMORTIZATION || type == REDEMPTION || type == TAX);
        }

        /**
         * Returns ISIN if description in "Name (ISIN)" format, null otherwise
         */
        public @Nullable String getSecurityIsin() {
            @SuppressWarnings("nullness")
            String securityDescription = requireNonNull(security);
            return SecurityHelper.getSecurityIsin(securityDescription);
        }

        /**
         * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
         */
        public String getSecurityName() {
            // Для Типа выплаты TAX может выдавать неверный тип бумаги,
            // но для текущего алгоритма SecurityHelper.getSecurityName() типа достаточно
            SecurityType securityType = switch (type) {
                case DIVIDEND -> SecurityType.SHARE;
                case ACCRUED_INTEREST, AMORTIZATION, REDEMPTION, COUPON -> SecurityType.BOND;
                case DERIVATIVE_PROFIT, DERIVATIVE_PRICE, DERIVATIVE_QUOTE -> SecurityType.DERIVATIVE;
                case TAX -> SecurityType.SHARE; // для TAX выдает не верный тип бумаги
                default -> throw new IllegalArgumentException("Не смог получить тип ЦБ по типу выплаты: " + type);
            };
            @SuppressWarnings("nullness")
            String securityDescription = requireNonNull(security);
            return SecurityHelper.getSecurityName(securityDescription, securityType);
        }
    }
}
