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

package ru.investbook.parser;

import java.io.InputStream;

public interface BrokerReportParserService {

    /**
     * Parse and backups report.
     * Method does not close input stream.
     *
     * @param inputStream      file content
     * @param fileName         file name
     * @param providedByBroker broker what generates report, may be null if unknown
     * @throws RuntimeException if report has broken format or parser not found
     */
    void parseReport(InputStream inputStream, String fileName, String providedByBroker);
}
