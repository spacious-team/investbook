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
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.converter.ExcelToHtmlConverter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import ru.investbook.report.ViewFilter;
import ru.investbook.report.excel.ExcelView;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

import static ru.investbook.report.html.ExcelFormulaEvaluatorHelper.evaluateFormulaCells;

@Component
@RequiredArgsConstructor
@Slf4j
public class HtmlView {
    private final ExcelView excelView;

    public void create(OutputStream out, ViewFilter filter) throws Exception {
        try (HSSFWorkbook workbook = createWorkbook(filter)) {
            Document htmlDocument = buildHtmlDocument(workbook);
            addCssStyle(htmlDocument);
            addReportFileDownloadLink(htmlDocument);
            addHomeLink(htmlDocument);
            write(htmlDocument, out);
        }
    }

    private HSSFWorkbook createWorkbook(ViewFilter filter) throws InterruptedException, ExecutionException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        excelView.writeTo(workbook, filter, new HtmlCellStyles(workbook));
        evaluateFormulaCells(workbook);
        setTitleAndAuthor(workbook);
        return workbook;
    }

    private void setTitleAndAuthor(HSSFWorkbook workbook) {
        workbook.createInformationProperties();
        SummaryInformation info = workbook.getSummaryInformation();
        info.setTitle("Отчет");
        info.setLastAuthor("Investbook");
    }

    private Document buildHtmlDocument(HSSFWorkbook workbook) throws ParserConfigurationException {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();
        ExcelToHtmlConverter excelToHtmlConverter = new ExcelToHtmlConverter(doc);
        excelToHtmlConverter.setOutputColumnHeaders(false);
        excelToHtmlConverter.setOutputRowNumbers(false);
        excelToHtmlConverter.processWorkbook(workbook);
        return excelToHtmlConverter.getDocument();
    }

    private void addCssStyle(Document htmlDocument) {
        Element style = htmlDocument.createElement("style");
        // @page for A4 landscape proportion when printing by ctrl+P
        style.setTextContent("""
                @page { size: 1980px 1400px landscape; }
                tr { border-bottom: 1pt solid #eee; }
                """);
        htmlDocument.getFirstChild() // html
                .getFirstChild()     // head
                .appendChild(style);
    }

    private void addReportFileDownloadLink(Document htmlDocument) {
        Node body = htmlDocument.getFirstChild() // html
                .getLastChild(); // body

        Element div = htmlDocument.createElement("div");
        div.setAttribute("style", "float: right");
        body.insertBefore(div, body.getFirstChild());

        String linkStyle = "text-decoration: none";
        Element link = htmlDocument.createElement("a");
        link.setTextContent("\uD83D\uDCE5 Сохранить в xlsx");
        link.setAttribute("href", "/portfolio/report?format=excel");
        link.setAttribute("style", linkStyle);
        div.appendChild(link);

        link = htmlDocument.createElement("a");
        link.setTextContent(", pdf");
        link.setAttribute("href", "#");
        link.setAttribute("onclick", "window.print()");
        link.setAttribute("style", linkStyle);
        div.appendChild(link);
    }

    private void addHomeLink(Document htmlDocument) {
        Node body = htmlDocument.getFirstChild() // html
                .getLastChild(); // body
        Element link = htmlDocument.createElement("a");
        link.setTextContent("[На главную]");
        link.setAttribute("href", "/");
        body.insertBefore(link, body.getFirstChild());
    }

    private void write(Document htmlDocument, OutputStream out) throws TransformerException {
        DOMSource domSource = new DOMSource(htmlDocument);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.transform(domSource, new StreamResult(out));
    }
}
