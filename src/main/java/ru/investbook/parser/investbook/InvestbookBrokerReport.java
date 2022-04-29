/*
 * InvestBook
 * Copyright (C) 2022  Vitalii Ananev <spacious-team@ya.ru>
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

package ru.investbook.parser.investbook;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.validator.internal.xml.CloseIgnoringInputStream;
import org.mozilla.universalchardet.UniversalDetector;
import org.spacious_team.broker.report_parser.api.BrokerReport;
import org.spacious_team.table_wrapper.api.ReportPage;
import org.spacious_team.table_wrapper.csv.CsvReportPage;
import org.spacious_team.table_wrapper.excel.ExcelSheet;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class InvestbookBrokerReport implements BrokerReport {

    @Getter
    private final ReportPage reportPage;
    private Workbook workbook;

    @SneakyThrows
    public InvestbookBrokerReport(String fileName, InputStream is) {
        if (fileName.endsWith(".csv")) {
            is.mark(Integer.MAX_VALUE);
            Charset charset = Optional.ofNullable(UniversalDetector.detectCharset(is))
                    .map(Charset::forName)
                    .orElse(StandardCharsets.UTF_8);
            is.reset();
            this.reportPage = new CsvReportPage(is, charset, CsvReportPage.getDefaultCsvParserSettings());
        } else {
            this.workbook = getWorkBook(fileName, is);
            this.reportPage = new ExcelSheet(workbook.getSheetAt(0));
            Assert.isTrue(reportPage.getRow(0).getCell(0).getStringValue().toLowerCase().contains("событие"),
                    "Не отчет в формате Investbook");
        }
    }

    public static Workbook getWorkBook(String excelFileName, InputStream is) {
        try {
            is = new CloseIgnoringInputStream(is); // HSSFWorkbook() constructor close is
            if (excelFileName.endsWith(".xls")) {
                return new HSSFWorkbook(is); // constructor close is
            } else {
                return new XSSFWorkbook(is);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не смог открыть excel файл", e);
        }
    }

    @Override
    public void close() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
    }
}
