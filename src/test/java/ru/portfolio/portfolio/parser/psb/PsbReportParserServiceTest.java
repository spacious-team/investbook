package ru.portfolio.portfolio.parser.psb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.portfolio.portfolio.PortfolioApplication;

@Ignore
@SpringBootTest(classes = PortfolioApplication.class)
public class PsbReportParserServiceTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private PsbReportParserService psbReportParserService;

    @DataProvider(name = "report")
    Object[][] getData() {
        return new Object[][] {{"E:\\1.xlsx"},
                {"E:\\2.xlsx"},
                {"E:\\Исполнение фьючерса.xlsx"},
                {"E:\\Налог.xlsx"}};
    }

    @Test(dataProvider = "report")
    void testParse(String report) {
        psbReportParserService.parse(report);
    }

}