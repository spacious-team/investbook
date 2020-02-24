package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class CashFlowTableTest {

    @DataProvider(name = "cash")
    Object[][] getData() {
        return new Object[][]{{"E:\\1.xlsx", BigDecimal.valueOf(400 - 760.77)},
                {"E:\\Налог.xlsx", BigDecimal.valueOf(-542.0)}};
    }

    @Test(dataProvider = "cash")
    void testIsin(String reportFile, BigDecimal expectedCashIn) throws IOException {
        PsbBrokerReport report = new PsbBrokerReport(reportFile);
        List<CashFlowTable.Row> data = new CashFlowTable(report).getData();
        BigDecimal sum = BigDecimal.ZERO;
        for (CashFlowTable.Row r : data) {
            sum = sum.add(r.getValue());
        }
        assertEquals(sum, expectedCashIn);
    }
}