/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Ignore
public class DerivativeCashFlowTableTest {

    @DataProvider(name = "cash-flow")
    Object[][] getData() {
        return new Object[][] {{"E:\\Исполнение фьючерса.xlsx", BigDecimal.valueOf(-733.0) }};
    }

    @Test(dataProvider = "cash-flow")
    void testIsin(String report, BigDecimal expectedSum) throws IOException {
        List<SecurityEventCashFlow> data = new DerivativeCashFlowTable(new PsbBrokerReport(report)).getData();
        BigDecimal sum = BigDecimal.ZERO;
        for (SecurityEventCashFlow r : data) {
            sum = sum.add(r.getValue());
        }
        assertEquals(sum, expectedSum);
    }
}