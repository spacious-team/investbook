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
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static java.time.ZoneId.systemDefault;

@Data
public class SecurityQuoteModel {

    private @Nullable Integer id;

    /**
     * In "name (isin)" or "contract-name" format
     */
    private @NotEmpty String security;

    private SecurityType securityType;

    private Instant timestamp = LocalDate.now().atTime(LocalTime.NOON).atZone(systemDefault()).toInstant();

    private @Positive BigDecimal quote;

    private @Nullable @Positive BigDecimal price;

    private @Nullable @PositiveOrZero BigDecimal accruedInterest;

    private @Nullable String currency;

    public void setSecurity(@Nullable String isin, @Nullable String securityName, SecurityType securityType) {
        this.security = SecurityHelper.getSecurityDescription(isin, securityName, securityType);
        this.securityType = securityType;
    }

    /**
     * Returns Name from template "Name (ISIN)" for stock and bond, code for derivative, securityName for asset
     */
    public String getSecurityName() {
        return SecurityHelper.getSecurityName(security, securityType);
    }

    /**
     * Returns ISIN if description in "Name (ISIN)" format, null otherwise
     */
    public @Nullable String getSecurityIsin() {
        return SecurityHelper.getSecurityIsin(security);
    }
}
