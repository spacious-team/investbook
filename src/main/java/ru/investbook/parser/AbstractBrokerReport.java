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

package ru.investbook.parser;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.spacious_team.table_wrapper.api.ReportPage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@ToString(of = "reportName")
@EqualsAndHashCode(of = "reportName")
public abstract class AbstractBrokerReport implements SingleBrokerReport {

    protected static final int LAST_TRADE_HOUR = 19;
    protected static final ZoneId MOSCOW_ZONEID = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter RUSSIAN_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final DateTimeFormatter dateFormatter = RUSSIAN_DATE_FORMATTER;
    private final DateTimeFormatter dateTimeFormatter = RUSSIAN_DATETIME_FORMATTER;
    @Getter
    private final ZoneId reportZoneId = MOSCOW_ZONEID;
    @Getter
    private final ReportPage reportPage;
    private final String reportName;
    @Getter
    private final Instant reportEndDateTime;
    @Getter
    private final String portfolio;
    @Getter
    private final SecurityRegistrar securityRegistrar;

    public AbstractBrokerReport(Attributes attributes, SecurityRegistrar securityRegistrar) {
        this.reportPage = attributes.reportPage();
        this.reportName = attributes.reportName();
        this.reportEndDateTime = attributes.reportEndDateTime();
        this.portfolio = attributes.portfolio();
        this.securityRegistrar = securityRegistrar;
    }

    public Instant convertToInstant(String value) {
        return convertToInstant(value, dateFormatter, dateTimeFormatter, reportZoneId);
    }

    protected static Instant convertToInstantWithRussianFormatAndMoscowZoneId(String value) {
        return convertToInstant(value, RUSSIAN_DATE_FORMATTER, RUSSIAN_DATETIME_FORMATTER, MOSCOW_ZONEID);
    }

    protected static Instant convertToInstant(String value,
                                              DateTimeFormatter dateFormatter,
                                              DateTimeFormatter dateTimeFormatter,
                                              ZoneId reportZoneId) {
        value = value.trim();
        if (value.contains(":")) {
            return LocalDateTime.parse(value, dateTimeFormatter).atZone(reportZoneId).toInstant();
        } else {
            return LocalDate.parse(value, dateFormatter).atStartOfDay(reportZoneId).toInstant();
        }
    }

    public record Attributes(ReportPage reportPage,
                             String reportName,
                             Instant reportEndDateTime,
                             String portfolio) {
    }
}
