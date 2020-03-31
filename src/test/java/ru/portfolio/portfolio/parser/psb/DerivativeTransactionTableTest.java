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
        assertEquals(data.get(0).getIsin(), firstIsin);
        assertEquals(data.get(data.size() - 1).getIsin(), lastIsin);
    }
}