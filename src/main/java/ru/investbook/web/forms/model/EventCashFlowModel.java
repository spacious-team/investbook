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

import lombok.Data;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.util.Objects.requireNonNull;
import static org.spacious_team.broker.pojo.CashFlowType.*;
import static org.springframework.util.StringUtils.hasLength;

@Data
public class EventCashFlowModel {

    @Nullable
    private Integer id;

    @NotEmpty
    private String portfolio;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    @NotNull
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime time = LocalTime.NOON;

    @NotNull
    private CashFlowType type;

    /**
     * Negative value for cash out, otherwise - positive
     */
    @NotNull
    private BigDecimal value;

    @NotEmpty
    private String valueCurrency = "RUB";

    @Nullable
    private String description;

    /**
     * Используется для привязки выплаты к бумаге из того же или другого счета
     */
    @Nullable
    private AttachedSecurity attachedSecurity = new AttachedSecurity();

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    public String getStringType() {
        if (type == null)  return null;
        return switch (type) {
            case DIVIDEND, COUPON, REDEMPTION, AMORTIZATION, ACCRUED_INTEREST -> type.name();
            case CASH -> {
                String desc = String.valueOf(description).toLowerCase();
                if (desc.contains("вычет") && desc.contains("иис")) {
                    yield "TAX_IIS_A";
                }
                yield type.name() + (isValuePositive() ? "_IN" : "_OUT");
            }
            default ->  type.name() + (isValuePositive() ? "_IN" : "_OUT");
        };
    }

    /**
     * Used by templates/events/edit-form.html
     */
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

        @Nullable
        private Integer securityEventCashFlowId;

        /**
         * In "name (isin)" or "contract-name" format
         */
        @Nullable
        private String security;

        @Nullable
        @Positive
        private Integer count;

        public boolean isValid() {
            return hasLength(security) &&
                    count != null && count > 0 &&
                    (type == DIVIDEND || type == COUPON || type == AMORTIZATION || type == REDEMPTION || type == TAX);
        }

        /**
         * Returns ISIN if description in "Name (ISIN)" format, null otherwise
         */
        public String getSecurityIsin() {
            return SecurityHelper.getSecurityIsin(requireNonNull(security));
        }

        /**
         * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
         */
        public String getSecurityName() {
            // Для Типа выплаты TAX может выдавать неверный тип бумаги,
            // но для текущего алгоритма SecurityHelper.getSecurityName() типа достаточно
            SecurityType securityType = switch(type) {
                case DIVIDEND -> SecurityType.SHARE;
                case ACCRUED_INTEREST, AMORTIZATION, REDEMPTION, COUPON -> SecurityType.BOND;
                case DERIVATIVE_PROFIT, DERIVATIVE_PRICE, DERIVATIVE_QUOTE -> SecurityType.DERIVATIVE;
                case TAX -> SecurityType.SHARE; // для TAX выдает не верный тип бумаги
                default -> throw new IllegalArgumentException("Не смог получить тип ЦБ по типу выплаты: " + type);
            };
            return SecurityHelper.getSecurityName(security, securityType);
        }
    }
}
