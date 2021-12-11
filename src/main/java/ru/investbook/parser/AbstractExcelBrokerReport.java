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

package ru.investbook.parser;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.CloseIgnoringInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractExcelBrokerReport extends AbstractBrokerReport {

    public AbstractExcelBrokerReport(SecurityRegistrar securityRegistrar) {
        super(securityRegistrar);
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

}
