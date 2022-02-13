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
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class SecurityDescriptionModel {

    @Nullable
    private Integer securityId;
    /**
     * In "name (isin)" or "contract-name" format
     */
    @NotEmpty
    private String security = "Наименование (XX0000000000)";

    @NotEmpty
    private String sector;

    @NotNull
    private SecurityType securityType;

    public void setSecurity(Integer securityId, String securityIsin, String securityName, SecurityType securityType) {
        this.securityId = securityId;
        this.security = SecurityHelper.getSecurityDescription(securityIsin, securityName, securityType);
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
    public String getSecurityIsin() {
        return SecurityHelper.getSecurityIsin(security);
    }
}
