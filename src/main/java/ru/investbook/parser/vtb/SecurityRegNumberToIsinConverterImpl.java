/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SecurityRegNumberToIsinConverterImpl implements SecurityRegNumberToIsinConverter {
    private final VtbSecuritiesTable vtbSecuritiesTable;
    private final VtbSecurityFlowTable vtbSecurityFlowTable;

    @Override
    public String convertToIsin(String registrationNumber) {
        if (registrationNumber == null) return null;
        registrationNumber = registrationNumber.toUpperCase();
        String isin = vtbSecurityFlowTable.getSecurityRegNumberToIsin().get(registrationNumber);
        if (isin == null) vtbSecuritiesTable.getSecurityRegNumberToIsin().get(registrationNumber);
        return (isin != null) ? isin.toUpperCase() : null;
    }
}
