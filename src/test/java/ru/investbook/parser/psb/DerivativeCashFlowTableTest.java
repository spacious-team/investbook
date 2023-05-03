/*
 * InvestBook
 * Copyright (C) 2020  Spacious Team <spacious-team@ya.ru>
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
import org.spacious_team.broker.pojo.SecurityEventCashFlow;
import ru.investbook.parser.SecurityRegistrar;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class DerivativeCashFlowTableTest {

    @Mock
    SecurityRegistrar securityRegistrar;

    static Object[][] cashFlow() {
        return new Object[][] {{"E:\\Исполнение фьючерса.xlsx", BigDecimal.valueOf(-733.0) }};
    }

    @ParameterizedTest
    @MethodSource("cashFlow")
    void testIsin(String report, BigDecimal expectedSum) throws IOException {
        PsbBrokerReport psbBrokerReport = new PsbBrokerReport(report, securityRegistrar);
        List<SecurityEventCashFlow> data = new DerivativeCashFlowTable(psbBrokerReport).getData();
        BigDecimal sum = BigDecimal.ZERO;
        for (SecurityEventCashFlow r : data) {
            sum = sum.add(r.getValue());
        }
        assertEquals(sum, expectedSum);
    }
}