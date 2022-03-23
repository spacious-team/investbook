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

package ru.investbook.web.forms.model;

import lombok.Data;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

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
    private AttachToSecurity attached = new AttachToSecurity();

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    public String getStringType() {
        if (type == null)  return null;
        return switch (type) {
            case DIVIDEND, COUPON, REDEMPTION, AMORTIZATION, ACCRUED_INTEREST -> type.name();
            case CASH -> {
                var desc = String.valueOf(description).toLowerCase();
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
        return attached != null && attached.isValid();
    }

    @Data
    public class AttachToSecurity {

        /**
         * In "name (isin)" or "contract-name" format
         */
        @Nullable
        private String security;

        @Nullable
        @Positive
        private Integer count;

        public boolean isValid() {
            return security != null && count != null && count > 0;
        }

        public SecurityEventCashFlowModel toSecurityEventCashFlowModel() {
            Assert.isTrue(isValid(), "Не указана бумага или количество для привязки выплаты");
            SecurityEventCashFlowModel m = new SecurityEventCashFlowModel();
            m.setPortfolio(portfolio);
            m.setDate(date);
            m.setTime(time);
            m.setSecurity(security);
            m.setCount(Objects.requireNonNull(count));
            if (type == CashFlowType.TAX) {
                // Используется для определения типа бумаги (BOND или SHARE) в SecurityEventCashFlowModel.getSecurityType()
                // Может не соответствовать действительности,
                // но для текущего алгоритма тип бумаги не имеет значения BOND или SHARE, поэтому указываем DIVIDEND
                m.setType(CashFlowType.DIVIDEND);
                m.setTax(value.abs()); // требуется положительное значение по контракту SecurityEventCashFlowModel.value
                m.setTaxCurrency(valueCurrency);
            } else {
                m.setType(type);
                m.setValue(value);
                m.setValueCurrency(valueCurrency);
            }
            return m;
        }
    }
}
