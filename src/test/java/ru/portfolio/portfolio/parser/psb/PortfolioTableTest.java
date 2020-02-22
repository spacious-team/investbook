package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

class PortfolioTableTest {
    PsbBrokerReport report;

    PortfolioTableTest() throws IOException {
        this.report = new PsbBrokerReport("E:\\1.xlsx");
    }

    @DataProvider(name = "isin")
    Object[][] getData() {
        return new Object[][] {{"RU000A0ZZDQ8", "RU000A0JV7J9" }};
    }

    @Test(dataProvider = "isin")
    void testIsin(String firstIsin, String lastIsin) {
        List<PortfolioTable.Row> data = new PortfolioTable(this.report).getData();
        assertEquals(data.get(0).getIsin(), firstIsin);
        assertEquals(data.get(data.size() - 1).getIsin(), lastIsin);
    }
}