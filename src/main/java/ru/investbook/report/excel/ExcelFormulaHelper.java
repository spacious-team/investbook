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

package ru.investbook.report.excel;

import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.ss.usermodel.Cell;

public class ExcelFormulaHelper {

    static String sumAbsValues(ExcelTableHeader column, int firstRowNum, int lastRowNum) {
        String range = column.getRange(firstRowNum, lastRowNum);
        return "=SUMIF(" + range + ",\">0\")-SUMIF(" + range + ",\"<0\")";
    }

    static String xirr(ExcelTableHeader cashColumn, ExcelTableHeader dateColumn,
                       int firstRowNum, int lastRowNum,
                       double devaultValueForApachePoi) {
        String excelXirrFormula = "XIRR("
                + cashColumn.getRange(firstRowNum, lastRowNum) + ","
                + dateColumn.getRange(firstRowNum, lastRowNum) + ")";
        return "=100*" + ifError(excelXirrFormula, devaultValueForApachePoi);
    }

    /**
     * {@link ru.investbook.report.html.HtmlView#handleNotImplementedException(Cell, HSSFFormulaEvaluator)}
     */
    static String ifError(String formula, Object defaultValueOrFormula) {
        return "IFERROR(" + formula + "," + defaultValueOrFormula + ")";
    }
}
