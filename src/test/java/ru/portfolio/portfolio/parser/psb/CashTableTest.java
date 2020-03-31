package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.testng.Assert.assertEquals;

@Ignore
public class CashTableTest {
    PsbBrokerReport report;

    CashTableTest() throws IOException {
        this.report = new PsbBrokerReport("E:\\1.xlsx");
    }

    @DataProvider(name = "cash_in")
    Object[][] getData() {
        return new Object[][] {{BigDecimal.valueOf(350.37)}};
    }

    @Test(dataProvider = "cash_in")
    void testIsin(BigDecimal expectedCash) {
        assertEquals(new CashTable(this.report).getData().get(0).getValue(), expectedCash);
    }
}