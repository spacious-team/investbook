package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Test(enabled = false)
public class TransactionTableTest {
    PsbBrokerReport report;

    TransactionTableTest() throws IOException {
        this.report = new PsbBrokerReport("E:\\1.xlsx");
    }

    @DataProvider(name = "isin")
    Object[][] getData() {
        return new Object[][] {{"RU000A0ZZYP6", "RU000A0JV4L2" }};
    }

    @Test(dataProvider = "isin")
    void testIsin(String firstIsin, String lastIsin) {
        List<TransactionTable.Row> data = new TransactionTable(this.report).getData();
        assertEquals(data.get(0).getIsin(), firstIsin);
        assertEquals(data.get(data.size() - 1).getIsin(), lastIsin);
    }
}