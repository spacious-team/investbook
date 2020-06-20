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
import ru.portfolio.portfolio.pojo.Security;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Ignore
public class PortfolioSecuritiesTableTest {

    @DataProvider(name = "isin")
    Object[][] getData() {
        return new Object[][] {{"E:\\1.xlsx", "RU000A0ZZDQ8", "RU000A0JV7J9" }};
    }

    @Test(dataProvider = "isin")
    void testIsin(String report, String firstIsin, String lastIsin) throws IOException {
        List<Security> data = new PortfolioSecuritiesTable(new PsbBrokerReport(report)).getData();
        assertEquals(data.get(0).getIsin(), firstIsin);
        assertEquals(data.get(data.size() - 1).getIsin(), lastIsin);
    }
}