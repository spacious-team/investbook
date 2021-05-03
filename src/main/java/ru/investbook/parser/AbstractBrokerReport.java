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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.spacious_team.table_wrapper.api.ReportPage;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@EqualsAndHashCode(of = "path")
public abstract class AbstractBrokerReport implements SingleBrokerReport {

    protected static final int LAST_TRADE_HOUR = 19;
    @Setter(AccessLevel.PROTECTED)
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    @Setter(AccessLevel.PROTECTED)
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Setter(AccessLevel.PROTECTED)
    private Path path;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private String portfolio;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private ReportPage reportPage;
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private Instant reportEndDateTime;
    @Getter
    private final ZoneId reportZoneId = ZoneId.of("Europe/Moscow");

    public Instant convertToInstant(String value) {
        value = value.trim();
        if (value.contains(":")) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(reportZoneId).toInstant();
        } else {
            return LocalDate.parse(value, dateFormatter).atStartOfDay(reportZoneId).toInstant();
        }
    }

    @Override
    public String toString() {
        return path.getFileName().toString();
    }
}
