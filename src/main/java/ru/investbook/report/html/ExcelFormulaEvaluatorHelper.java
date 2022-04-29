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

package ru.investbook.report.html;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.util.Assert;

@Slf4j
class ExcelFormulaEvaluatorHelper {

    static void evaluateFormulaCells(HSSFWorkbook workbook) {
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(workbook);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == CellType.FORMULA) {
                        evaluate(c, evaluator);
                    }
                }
            }
        }
    }

    private static void evaluate(Cell c, HSSFFormulaEvaluator evaluator) {
        try {
            evaluator.evaluateFormulaCell(c);
        } catch (NotImplementedException e) {
            if (!handleNotImplementedException(c, evaluator)) {
                log.warn("Can't evaluate cell formula {}", c.getCellFormula(), e);
            }
        } catch (Exception e) {
            log.warn("Can't evaluate cell formula {}", c.getCellFormula(), e);
        }
    }

    /**
     * Если формула соответствует шаблону '{A}IFERROR({B},{default-value}){C}', то будет попытка вычислить значение '{A}{default-value}{C}'
     */
    private static boolean handleNotImplementedException(Cell c, HSSFFormulaEvaluator evaluator) {
        try {
            String formula = c.getCellFormula();
            int ifErrorFuncStartPos = formula.toUpperCase().indexOf("IFERROR(");
            if (ifErrorFuncStartPos != -1) {
                int openBracePos = formula.indexOf("(", ifErrorFuncStartPos);
                int ifErrorFuncEndPos = indexOfCloseBrace(formula, openBracePos);
                if (ifErrorFuncEndPos != -1) {
                    int positionOfSecondArg = indexOfSecondArg(formula, openBracePos);
                    String defaultValueOrFunction = formula.substring(positionOfSecondArg, ifErrorFuncEndPos);
                    String newFormula = formula.substring(0, ifErrorFuncStartPos) +
                            defaultValueOrFunction +
                            formula.substring(Math.min(ifErrorFuncEndPos + 1, formula.length()));
                    c.setCellFormula(newFormula);
                    evaluate(c, evaluator);
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    private static int indexOfCloseBrace(String formula, int openBracePos) {
        Assert.isTrue(formula.charAt(openBracePos) == '(', "Open brace expected");
        int braceCnt = 1;
        for (int i = openBracePos + 1; i < formula.length(); i++) {
            char c = formula.charAt(i);
            if (c == '(') {
                braceCnt++;
            } else if (c == ')') {
                braceCnt--;
            }
            if (braceCnt == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfSecondArg(String formula, int openBracePos) {
        Assert.isTrue(formula.charAt(openBracePos) == '(', "Open brace expected");
        int braceCnt = 0;
        for (int i = openBracePos + 1; i < formula.length(); i++) {
            char c = formula.charAt(i);
            if (c == ',' && braceCnt == 0) {
                return i + 1;
            } else if (c == '(') {
                braceCnt++;
            } else if (c == ')') {
                braceCnt--;
            }
        }
        return -1;
    }
}
