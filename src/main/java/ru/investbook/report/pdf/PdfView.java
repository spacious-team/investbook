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

package ru.investbook.report.pdf;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.styledxmlparser.jsoup.Jsoup;
import com.itextpdf.styledxmlparser.jsoup.nodes.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.investbook.report.ViewFilter;
import ru.investbook.report.html.HtmlView;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class PdfView {
    private final HtmlView htmlView;

    public void create(OutputStream out, ViewFilter filter) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(128 * 1024)) {
            htmlView.create(os, filter);
            //ByteArrayInputStream in = new ByteArrayInputStream(os.toByteArray());
            Document htmlDoc = Jsoup.parse(os.toString(StandardCharsets.UTF_8));
            // A4 landscape proportion
            htmlDoc.head().append("""
                    <style>
                    @page { size: 1980px 1395px; }
                    </style>
                    """);
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(htmlDoc.outerHtml(), out, properties);
        }
    }

}
