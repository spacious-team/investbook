/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
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

package ru.investbook.parser.sber.transaction;

import org.apache.poi.ss.usermodel.Workbook;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import ru.investbook.parser.AbstractExcelBrokerReport;

import java.io.InputStream;
import java.nio.file.Paths;

public class SberTrBrokerReport extends AbstractExcelBrokerReport {

    private final Workbook book;

    public SberTrBrokerReport(String excelFileName, InputStream is) {
        this.book = getWorkBook(excelFileName, is);
        ExcelSheet reportPage = new ExcelSheet(book.getSheetAt(0));
        checkReportFormat(excelFileName, reportPage);
        setPath(Paths.get(excelFileName));
        setReportPage(reportPage);
    }

    public static void checkReportFormat(String excelFileName, ExcelSheet reportPage) {
        if (reportPage.getSheet().getSheetName().equals("Сделки") &&
                reportPage.getRow(0).getCell(0).getStringValue().equals("Номер договора")) {
            return;
        }
        throw new RuntimeException("В файле " + excelFileName + " не содержится отчета сделок брокера Сбербанк");
    }

    @Override
    public void close() throws Exception {
        this.book.close();
    }
}
