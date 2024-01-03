/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.parser.psb;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import ru.investbook.parser.SecurityRegistrar;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class PortfolioCashTableTest {

    @Mock
    SecurityRegistrar securityRegistrar;

    static Object[][] cashIn() {
        return new Object[][] {{"E:\\1.xlsx", BigDecimal.valueOf(350.37)}};
    }

    @ParameterizedTest
    @MethodSource("cashIn")
    void testIsin(String report, BigDecimal expectedCash) throws IOException {
        PsbBrokerReport psbBrokerReport = new PsbBrokerReport(report, securityRegistrar);
        assertEquals(new CashTable(psbBrokerReport).getData().get(0).getValue(), expectedCash);
    }
}