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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static java.time.ZoneId.systemDefault;

@Data
public class SecurityQuoteModel {

    @Nullable
    private Integer id;

    /**
     * In "name (isin)" or "contract-name" format
     */
    @NotEmpty
    private String security;

    @NotNull
    private SecurityType securityType;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant timestamp = LocalDate.now().atTime(12, 0).atZone(systemDefault()).toInstant();

    @NotNull
    @Positive
    private BigDecimal quote;

    @Positive
    private BigDecimal price;

    @Positive
    private BigDecimal accruedInterest;

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
