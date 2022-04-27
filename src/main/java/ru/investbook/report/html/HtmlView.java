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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.apache.poi.hssf.usermodel.HSSFFormulaEvaluator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ru.investbook.report.ViewFilter;
import ru.investbook.report.excel.ExcelView;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class HtmlView {
    private final ExcelView excelView;

    public void create(OutputStream out, ViewFilter filter) throws Exception {
        try (HSSFWorkbook workbook = new HSSFWorkbook()) {

            excelView.writeTo(workbook, filter);
            evaluateAllFormulaCells(workbook);

            ExcelToHtmlConverter excelToHtmlConverter = new ExcelToHtmlConverter(
                    DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .newDocument());
            excelToHtmlConverter.setOutputColumnHeaders(false);
            excelToHtmlConverter.setOutputRowNumbers(false);

            excelToHtmlConverter.processWorkbook(workbook);

            Document htmlDocument = excelToHtmlConverter.getDocument();

            Element style = htmlDocument.createElement("style");
            style.setTextContent(".r1 { border-bottom: 1pt solid #eee; }");
            htmlDocument.getFirstChild() // html
                    .getFirstChild()     // head
                    .appendChild(style);

            DOMSource domSource = new DOMSource(htmlDocument);
            StreamResult streamResult = new StreamResult(out);

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.METHOD, "html");
            serializer.transform(domSource, streamResult);
        }
    }

    private void evaluateAllFormulaCells(HSSFWorkbook workbook) {
        HSSFFormulaEvaluator evaluator = new HSSFFormulaEvaluator(workbook);
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);

            for (Row r : sheet) {
                for (Cell c : r) {
                    if (c.getCellType() == CellType.FORMULA) {
                        try {
                            c.isPartOfArrayFormulaGroup();
                            evaluator.evaluateFormulaCell(c);
                        } catch (Exception e) {
                            log.warn("", e);
                        }
                    }
                }
            }
        }
    }
}
