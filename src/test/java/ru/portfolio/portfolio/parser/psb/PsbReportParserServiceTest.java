package ru.portfolio.portfolio.parser.psb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import ru.portfolio.portfolio.PortfolioApplication;

@SpringBootTest(classes = PortfolioApplication.class)
public class PsbReportParserServiceTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private PsbReportParserService psbReportParserService;

    @Test
    void testParse() {
        psbReportParserService.parse("E:\\1.xlsx");
    }

}