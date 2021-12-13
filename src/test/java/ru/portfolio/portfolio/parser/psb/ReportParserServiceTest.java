/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser.psb;

import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import ru.investbook.InvestbookApplication;
import ru.investbook.parser.ReportParserService;
import ru.investbook.parser.SecurityRegistrar;

import java.io.IOException;
import java.nio.file.Paths;

@Ignore
@SpringBootTest(classes = InvestbookApplication.class)
public class ReportParserServiceTest extends AbstractTestNGSpringContextTests {

    @Mock
    SecurityRegistrar securityRegistrar;

    @Autowired
    private ReportParserService reportParserService;

    @DataProvider(name = "report")
    Object[][] getData() {
        return new Object[][] {{"E:\\1.xlsx"},
                {"E:\\2.xlsx"},
                {"E:\\Исполнение фьючерса.xlsx"},
                {"E:\\Налог.xlsx"}};
    }

    @Test(dataProvider = "report")
    void testParse(String report) throws IOException {
        PsbBrokerReport brokerReport = new PsbBrokerReport(Paths.get(report), securityRegistrar);
        PsbReportTables reportTableFactory = new PsbReportTables(brokerReport);
        reportParserService.parse(reportTableFactory);
    }

}