/*
 * InvestBook
 * Copyright (C) 2023  Spacious Team <spacious-team@ya.ru>
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

package ru.investbook.upgrade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlDataExporter {
    public static final String file = "~/investbook/export-2022.9.sql";
    private final JdbcTemplate jdbcTemplate;

    @PreDestroy
    public void export() {
        try {
            Instant t0 = Instant.now();
            jdbcTemplate.execute("SCRIPT DROP TO '" + file + "' CHARSET 'UTF-8'");
            Instant t1 = Instant.now();
            log.info("Экспорт БД для импорта в Investbook 2023.1 завершен за {}", Duration.between(t0, t1));
        } catch (Exception e) {
            log.error("Не смог выполнить экспорт БД для импорта в Investbook 2023.1", e);
        }
    }
}
