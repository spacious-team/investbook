package ru.portfolio.portfolio.parser.psb;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class DerivativeCashFlowTableTest {

    @DataProvider(name = "cash-flow")
    Object[][] getData() {
        return new Object[][] {{"E:\\Исполнение фьючерса.xlsx", BigDecimal.valueOf(-733.0) }};
    }

    @Test(dataProvider = "cash-flow")
    void testIsin(String report, BigDecimal expectedSum) throws IOException {
        List<DerivativeCashFlowTable.Row> data = new DerivativeCashFlowTable(new PsbBrokerReport(report)).getData();
        BigDecimal sum = BigDecimal.ZERO;
        for (DerivativeCashFlowTable.Row r : data) {
            sum = sum.add(r.getValue());
        }
        assertEquals(sum, expectedSum);
    }
}