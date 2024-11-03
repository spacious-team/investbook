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

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.investbook.InvestbookProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlDataImporter {
    private final InvestbookProperties properties;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        properties.getSqlImportFiles()
                .forEach(this::importFile);
    }

    private void importFile(Path path) {
        if (Files.exists(path)) {
            boolean isSuccess = importSqlData(path);
            if (isSuccess) {
                renameExportFile(path);
            }
        }
    }

    private boolean importSqlData(Path path) {
        try {
            Instant t0 = Instant.now();
            Path absolutePath = path.toAbsolutePath().normalize();
            String command = "RUNSCRIPT FROM '" + absolutePath + "' CHARSET 'UTF-8'";
            boolean isH2v1_xFile = String.valueOf(path.getFileName()).contains("2022.9");
            if (isH2v1_xFile) {
                command += " FROM_1X";
            }
            jdbcTemplate.execute(command);
            Instant t1 = Instant.now();
            log.info("Импорт данных предыдущей версии Investbook из файла '{}' завершен за {}", path, Duration.between(t0, t1));
            return true;
        } catch (Exception e) {
            log.error("Не смог выполнить импорт данных предыдущей версии Investbook из файла '{}'", path, e);
            return false;
        }
    }

    private void renameExportFile(Path path) {
        Path importedFile = Path.of(path + ".done");
        try {
            Files.move(path, importedFile);
        } catch (Exception e) {
            throw new RuntimeException("Не смог переименовать файл импорта '" + path + "' в '" + importedFile + "'. " +
                    "Переименуйте вручную и запустите Investbook повторно во избежание потери данных", e);
        }
    }
}
