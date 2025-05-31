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

import org.spacious_team.broker.report_parser.api.BrokerReportFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ReportControllerHelper {

   static List<String> getBrokerNames(Collection<BrokerReportFactory> brokerReportFactories) {
        return brokerReportFactories.stream()
                .map(BrokerReportFactory::getBrokerName)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    public static ResponseEntity<String> errorPage(String title, Collection<Exception> exceptions) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exceptions.stream()
                        .map(e -> {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            e.printStackTrace(pw);
                            return sw.toString().replace("\n", "</br>");
                        }).collect(errorMessageBuilder(title)));
    }

    private static Collector<CharSequence, ?, String> errorMessageBuilder(String title) {
        return Collectors.joining("</br></br> - ", """
                <b>Ошибка загрузки</b> <a href="/">[на главную]</a><br><br>
                """ + title + """
                <br>
                <span style="font-size: smaller; color: gray;">Вы можете
                <a href="https://github.com/spacious-team/investbook/issues/new?labels=bug&template=bug_report.md">сообщить</a>
                об ошибке разработчикам  или связаться с <a href="https://t.me/+zriyX7tRQOc0MDEy">технической поддержкой</a>
                </span>
                <br><br> -
                """, "");
    }
}