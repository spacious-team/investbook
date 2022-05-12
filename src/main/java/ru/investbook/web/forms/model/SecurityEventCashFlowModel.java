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
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SecurityEventCashFlowModel {

    @Nullable
    private Integer id;

    @Nullable
    private Integer taxId;

    @NotEmpty
    private String portfolio;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date = LocalDate.now();

    @NotNull
    @DateTimeFormat(pattern = "HH:mm:ss")
    private LocalTime time = LocalTime.NOON;

    /**
     * In "name (isin)" or "contract-name" format
     */
    @NotEmpty
    private String security;

    @NotNull
    private int count;

    @NotNull
    private CashFlowType type;

    @NotNull
    private BigDecimal value;

    @NotEmpty
    private String valueCurrency = "RUB";

    @Nullable
    @PositiveOrZero
    private BigDecimal tax;

    @Nullable
    private String taxCurrency = "RUB";

    public void setValueCurrency(String currency) {
        this.valueCurrency = currency.toUpperCase();
    }

    public void setTaxCurrency(String currency) {
        this.taxCurrency = currency.toUpperCase();
    }

    public void setSecurity(String isin, String securityName, SecurityType securityType) {
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
    public String getSecurityIsin() {
        return SecurityHelper.getSecurityIsin(security);
    }

    public SecurityType getSecurityType() {
        return switch(type) {
            case DIVIDEND -> SecurityType.SHARE;
            case ACCRUED_INTEREST, AMORTIZATION, REDEMPTION, COUPON -> SecurityType.BOND;
            case DERIVATIVE_PROFIT, DERIVATIVE_PRICE, DERIVATIVE_QUOTE -> SecurityType.DERIVATIVE;
            default -> throw new IllegalArgumentException("Не смог получить тип ЦБ по типу выплаты: " + type);
        };
    }
}
