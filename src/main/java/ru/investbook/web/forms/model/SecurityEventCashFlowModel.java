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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SecurityEventCashFlowModel {

    private @Nullable Integer id;

    private @Nullable Integer taxId;

    private @NotEmpty String portfolio;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private @NotNull LocalDate date = LocalDate.now();

    @DateTimeFormat(pattern = "HH:mm:ss")
    private @NotNull LocalTime time = LocalTime.NOON;

    /**
     * In "name (isin)" or "contract-name" format
     */
    private @NotEmpty String security;

    private int count;

    private @NotNull CashFlowType type;

    private @NotNull BigDecimal value;

    private @NotEmpty String valueCurrency = "RUB";

    private @Nullable @PositiveOrZero BigDecimal tax;

    private @Nullable String taxCurrency = "RUB";

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    public void setTaxCurrency(String currency) {
        this.taxCurrency = currency.toUpperCase();
    }

    public void setSecurity(@Nullable String isin, @Nullable String securityName, SecurityType securityType) {
        this.security = SecurityHelper.getSecurityDescription(isin, securityName, securityType);
    }

    /**
     * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
     */
    public String getSecurityName() {
        return SecurityHelper.getSecurityName(security, getSecurityType());
    }

    /**
     * Returns ISIN if description in "Name (ISIN)" format, null otherwise
     */
    public @Nullable String getSecurityIsin() {
        return SecurityHelper.getSecurityIsin(security);
    }

    public SecurityType getSecurityType() {
        return switch (type) {
            case DIVIDEND -> SecurityType.SHARE;
            case ACCRUED_INTEREST, AMORTIZATION, REDEMPTION, COUPON -> SecurityType.BOND;
            case DERIVATIVE_PROFIT, DERIVATIVE_PRICE, DERIVATIVE_QUOTE -> SecurityType.DERIVATIVE;
            default -> throw new IllegalArgumentException("Не смог получить тип ЦБ по типу выплаты: " + type);
        };
    }
}
