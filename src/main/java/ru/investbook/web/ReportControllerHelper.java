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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class ReportControllerHelper {

    static List<String> getBrokerNames(Collection<BrokerReportFactory> brokerReportFactories) {
        return brokerReportFactories.stream()
                .map(BrokerReportFactory::getBrokerName)
                .map(String::toLowerCase)
                .distinct()
                .toList();
    }

    public static String exceptionToString(Exception exception) {
        return exceptionsToString(Set.of(exception));
    }

    public static String exceptionsToString(Collection<Exception> exceptions) {
        return exceptions.stream()
                .map(ReportControllerHelper::exceptionToStringConverter)
                .collect(joining("\n\n -", "-", ""));
    }

    private static String exceptionToStringConverter(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}