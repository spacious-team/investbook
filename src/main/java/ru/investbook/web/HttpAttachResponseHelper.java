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

package ru.investbook.web;

import org.springframework.http.ContentDisposition;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class HttpAttachResponseHelper {

    public static void sendSuccessHeader(HttpServletResponse response, String fileName, String contentType) {
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        response.setHeader("Content-disposition", contentDisposition.toString());
        response.setContentType(contentType);
    }

    public static void sendErrorPage(HttpServletResponse response, Exception e) throws IOException {
        sendErrorHttpHeader(response);
        String httpBody = getErrorHttpBody(e);
        response.getWriter().write(httpBody);
    }

    private static String getErrorHttpBody(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return Stream.of(sw.toString().split("\n"))
                .collect(joining("</br>", """
                        <b>Ошибка сборки отчета</b></br></br> <a href="/">[назад]</a>
                        <br/>
                        <span style="font-size: smaller; color: gray;">
                            Вы можете <a href="https://github.com/spacious-team/investbook/issues">сообщить</a>
                            об ошибке разработчикам или связаться с
                            <a href="https://t.me/investbook_support">технической поддержкой</a> 
                        </span>
                        </br></br> - 
                        """, ""));
    }

    private static void sendErrorHttpHeader(HttpServletResponse response) {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
