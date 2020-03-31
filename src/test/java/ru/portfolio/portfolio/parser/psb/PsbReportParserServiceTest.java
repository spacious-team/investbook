/*
 * Portfolio
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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