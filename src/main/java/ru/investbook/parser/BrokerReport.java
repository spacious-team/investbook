/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.parser;

import ru.investbook.parser.table.ReportPage;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

public interface BrokerReport extends AutoCloseable {
    ReportPage getReportPage();
    String getPortfolio();
    Path getPath();
    Instant getReportEndDateTime();
    ZoneId getReportZoneId();
    Instant convertToInstant(String value);
}
