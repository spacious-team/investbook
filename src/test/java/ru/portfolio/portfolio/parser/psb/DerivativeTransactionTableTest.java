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

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Ignore
public class DerivativeTransactionTableTest {

    @DataProvider(name = "isin")
    Object[][] getData() {
        return new Object[][] {{"E:\\Исполнение фьючерса.xlsx", "Si-12.19M191219CA65500", "Si-12.19" }};
    }

    @Test(dataProvider = "isin")
    void testIsin(String report, String firstIsin, String lastIsin) throws IOException {
        List<DerivativeTransactionTable.FortsTableRow> data = new DerivativeTransactionTable(new PsbBrokerReport(report)).getData();
        assertEquals(data.get(0).getContract(), firstIsin);
        assertEquals(data.get(data.size() - 1).getContract(), lastIsin);
    }
}