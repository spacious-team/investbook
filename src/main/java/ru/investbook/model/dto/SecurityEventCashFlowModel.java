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

package ru.investbook.model.dto;

import lombok.Data;
import org.spacious_team.broker.pojo.CashFlowType;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    @Positive
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

    public void setSecurity(String securityId, String securityName) {
        this.security = SecurityHelper.getSecurityDescription(securityId, securityName);
    }

    /**
     * Returns ISIN (stock market) or contract name (derivatives and forex market)
     */
    public String getSecurityId() {
        return SecurityHelper.getSecurityId(security);
    }

    /**
     * Returns security name (stock market) or null (derivatives and forex market)
     */
    public String getSecurityName() {
        return SecurityHelper.getSecurityName(security);
    }

    public String getSecurityDisplayName() {
        return SecurityHelper.getSecurityDisplayName(security);
    }
}
