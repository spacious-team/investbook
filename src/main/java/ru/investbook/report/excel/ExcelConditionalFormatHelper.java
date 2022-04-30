/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import org.apache.poi.ss.usermodel.BorderFormatting;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;

class ExcelConditionalFormatHelper {
    private static final byte[] backgroundRed = new byte[]{(byte) 255, (byte) 200, (byte) 200};
    private static final byte[] borderRed = new byte[]{(byte) 255, (byte) 175, (byte) 175};

    static void highlightNegativeByRed(Sheet sheet, ExcelTableHeader column) {
        if (!(sheet instanceof XSSFSheet)) {
            return;
        }
        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        IndexedColorMap indexedColors = ((XSSFSheet) sheet).getWorkbook()
                .getStylesSource()
                .getIndexedColors();

        CellRangeAddress range =
                new CellRangeAddress(2, sheet.getLastRowNum(), column.ordinal(), column.ordinal());

        ConditionalFormattingRule rule = sheetCF
                .createConditionalFormattingRule(column.getCellAddr(3) + "<0");

        PatternFormatting patternFormatting = rule.createPatternFormatting();
        patternFormatting.setFillBackgroundColor(new XSSFColor(backgroundRed, indexedColors));
        patternFormatting.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        XSSFColor borderColor = new XSSFColor(borderRed, indexedColors);
        BorderFormatting borderFormatting = rule.createBorderFormatting();
        borderFormatting.setTopBorderColor(borderColor);
        borderFormatting.setBottomBorderColor(borderColor);
        borderFormatting.setLeftBorderColor(borderColor);
        borderFormatting.setRightBorderColor(borderColor);

        borderFormatting.setBorderTop(BorderStyle.THIN);
        borderFormatting.setBorderBottom(BorderStyle.THIN);
        borderFormatting.setBorderLeft(BorderStyle.THIN);
        borderFormatting.setBorderRight(BorderStyle.THIN);

        sheetCF.addConditionalFormatting(new CellRangeAddress[]{range}, rule);
    }
}
