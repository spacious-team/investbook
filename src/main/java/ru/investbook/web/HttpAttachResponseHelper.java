/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;

public class HttpAttachResponseHelper {

    public static void sendSuccessHeader(HttpServletResponse response, String fileName, String contentType) {
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName, StandardCharsets.UTF_8)
                .build();
        response.setHeader("Content-disposition", contentDisposition.toString());
        response.setContentType(contentType);
    }

    public static void sendErrorHttpHeader(HttpServletResponse response) {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
