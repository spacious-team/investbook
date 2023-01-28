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
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.investbook.InvestbookProperties;

import javax.annotation.PreDestroy;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlDataExporter {
    private static final String EXPECTED_INVESTBOOK_VERSION_FOR_EXPORT = "2022.9";
    private final BuildProperties buildProperties;
    private final InvestbookProperties investbookProperties;
    private final JdbcTemplate jdbcTemplate;

    @PreDestroy
    public void preDestroy() {
        String version = buildProperties.getVersion();
        if (Objects.equals(version, EXPECTED_INVESTBOOK_VERSION_FOR_EXPORT)) {
            Path file = investbookProperties.getDataPath()
                    .resolve("export-" + version + ".sql")
                    .toAbsolutePath();
            exportSqlData(file);
        }
    }

    private void exportSqlData(Path file) {
        try {
            Instant t0 = Instant.now();
            jdbcTemplate.execute("SCRIPT DROP TO '" + file + "' CHARSET 'UTF-8'");
            Instant t1 = Instant.now();
            log.info("Экспорт БД в файл '{}' завершен за {}", file, Duration.between(t0, t1));
        } catch (Exception e) {
            log.error("Не смог выполнить экспорт БД в файл '{}'", file, e);
        }
    }
}
