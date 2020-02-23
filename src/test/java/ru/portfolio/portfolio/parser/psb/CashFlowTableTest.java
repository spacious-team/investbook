package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class CashFlowTableTest {
    PsbBrokerReport report;

    CashFlowTableTest() throws IOException {
        this.report = new PsbBrokerReport("E:\\1.xlsx");
    }

    @DataProvider(name = "cash_in")
    Object[][] getData() {
        return new Object[][] {{BigDecimal.valueOf(1)}};
    }

    @Test(dataProvider = "cash_in")
    void testIsin(BigDecimal expectedCashIn) {
        List<CashFlowTable.Row> data = new CashFlowTable(this.report).getData();
        BigDecimal sum = BigDecimal.valueOf(0);
        for (CashFlowTable.Row r : data) {
            sum = sum.add(r.getValue());
        }
        assertEquals(sum, expectedCashIn);
    }
}